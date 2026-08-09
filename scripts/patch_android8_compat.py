#!/usr/bin/env python3
"""Patch MediaPipe's API-28 aligned_alloc import for Android 8.x.

MediaPipe tasks-vision 1.0.0 imports aligned_alloc@LIBC_P. Android 8.1
(API 27) does not export that symbol, while memalign has the same two-argument
ABI and is available on old Android releases. This tool renames the undefined
dynamic symbol to memalign and clears its API-28 symbol-version requirement.
"""

from __future__ import annotations

import argparse
import pathlib
import struct
import sys


SHT_DYNSYM = 11
SHT_GNU_VERSYM = 0x6FFFFFFF
OLD_SYMBOL = b"aligned_alloc"
NEW_SYMBOL = b"memalign"
VER_NDX_GLOBAL = 1


class ElfError(RuntimeError):
    pass


def _cstring(data: bytearray, start: int) -> bytes:
    end = data.find(b"\0", start)
    if end < 0:
        raise ElfError("unterminated string in ELF string table")
    return bytes(data[start:end])


def patch_elf(path: pathlib.Path, check_only: bool = False) -> int:
    data = bytearray(path.read_bytes())
    if data[:4] != b"\x7fELF":
        raise ElfError(f"{path}: not an ELF file")
    if data[5] != 1:
        raise ElfError(f"{path}: only little-endian ELF files are supported")

    elf_class = data[4]
    if elf_class == 1:
        section_offset = struct.unpack_from("<I", data, 32)[0]
        section_entry_size = struct.unpack_from("<H", data, 46)[0]
        section_count = struct.unpack_from("<H", data, 48)[0]
        section_fmt = "<IIIIIIIIII"
        symbol_name_offset = 0
    elif elf_class == 2:
        section_offset = struct.unpack_from("<Q", data, 40)[0]
        section_entry_size = struct.unpack_from("<H", data, 58)[0]
        section_count = struct.unpack_from("<H", data, 60)[0]
        section_fmt = "<IIQQQQIIQQ"
        symbol_name_offset = 0
    else:
        raise ElfError(f"{path}: unsupported ELF class {elf_class}")

    expected_section_size = struct.calcsize(section_fmt)
    if section_entry_size < expected_section_size:
        raise ElfError(f"{path}: invalid ELF section header size")

    sections: list[dict[str, int]] = []
    for index in range(section_count):
        values = struct.unpack_from(section_fmt, data, section_offset + index * section_entry_size)
        if elf_class == 1:
            _, section_type, _, _, offset, size, link, _, _, entry_size = values
        else:
            _, section_type, _, _, offset, size, link, _, _, entry_size = values
        sections.append(
            {
                "type": section_type,
                "offset": offset,
                "size": size,
                "link": link,
                "entry_size": entry_size,
            }
        )

    dynsym_index = next((i for i, s in enumerate(sections) if s["type"] == SHT_DYNSYM), None)
    versym = next((s for s in sections if s["type"] == SHT_GNU_VERSYM), None)
    if dynsym_index is None or versym is None:
        raise ElfError(f"{path}: missing dynamic symbol/version sections")

    dynsym = sections[dynsym_index]
    if dynsym["link"] >= len(sections):
        raise ElfError(f"{path}: invalid dynamic string table link")
    dynstr = sections[dynsym["link"]]
    symbol_entry_size = dynsym["entry_size"] or (16 if elf_class == 1 else 24)
    symbol_count = dynsym["size"] // symbol_entry_size

    old_indexes: list[int] = []
    compatible_indexes: list[int] = []
    for symbol_index in range(symbol_count):
        symbol_offset = dynsym["offset"] + symbol_index * symbol_entry_size
        name_index = struct.unpack_from("<I", data, symbol_offset + symbol_name_offset)[0]
        if name_index >= dynstr["size"]:
            continue
        name_offset = dynstr["offset"] + name_index
        name = _cstring(data, name_offset)
        version = struct.unpack_from("<H", data, versym["offset"] + symbol_index * 2)[0] & 0x7FFF
        if name == OLD_SYMBOL:
            old_indexes.append(symbol_index)
            if not check_only:
                replacement = NEW_SYMBOL + b"\0" * (len(OLD_SYMBOL) - len(NEW_SYMBOL))
                data[name_offset : name_offset + len(OLD_SYMBOL)] = replacement
                struct.pack_into("<H", data, versym["offset"] + symbol_index * 2, VER_NDX_GLOBAL)
        elif name == NEW_SYMBOL and version == VER_NDX_GLOBAL:
            compatible_indexes.append(symbol_index)

    if check_only:
        if old_indexes:
            raise ElfError(f"{path}: still imports aligned_alloc")
        if not compatible_indexes:
            raise ElfError(f"{path}: compatible unversioned memalign import not found")
        print(f"OK {path}: Android 8 compatibility symbol verified")
        return 0

    if not old_indexes:
        if compatible_indexes:
            print(f"SKIP {path}: already patched")
            return 0
        raise ElfError(f"{path}: aligned_alloc import not found")

    path.write_bytes(data)
    print(f"PATCHED {path}: {len(old_indexes)} symbol(s)")
    return len(old_indexes)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="verify without changing files")
    parser.add_argument("files", nargs="+", type=pathlib.Path)
    args = parser.parse_args()

    try:
        for file_path in args.files:
            patch_elf(file_path, check_only=args.check)
    except (ElfError, OSError, struct.error) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
