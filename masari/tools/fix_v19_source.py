from pathlib import Path
import re

p = Path('masari/app/src/main/java/com/masari/personalplan/MasariV19Activity.java')
s = p.read_text(encoding='utf-8')
s = s.replace('t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD););', 't.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));')
s = re.sub(r'\breturn(\d+);', r'return \1;', s)
s = s.replace('if(!medicineActive())removeTextParent(root,"طب");renameText(root,"لاحقًا","أفكار لاحقًا");', 'if(!medicineActive())hideExactText(root,"طب");renameText(root,"لاحقًا","أفكار لاحقًا");')
needle = '    private void findTaskCards(View v,List<View>out)'
helper = '''    private void hideExactText(View v,String exact){for(TextView t:texts(v))if(exact.equals(String.valueOf(t.getText()))){t.setVisibility(View.GONE);return;}}\n'''
if needle not in s:
    raise SystemExit('v19 helper insert point missing')
s = s.replace(needle, helper + needle, 1)
p.write_text(s, encoding='utf-8')
print('v19 source fixes applied')
