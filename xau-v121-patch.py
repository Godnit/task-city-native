from pathlib import Path
import re
root=Path('.')
app=root/'app/src/main/assets/app.js'
grad=root/'app/build.gradle'
js=app.read_text()
g=grad.read_text()

def req(old,new,count=1):
    global js
    if old not in js:
        raise SystemExit('missing pattern: '+old[:100].replace('\n',' '))
    js=js.replace(old,new,count)

# Version
g=g.replace('versionCode 21','versionCode 22').replace("versionName '1.20.0'","versionName '1.21.0'")
if 'versionCode 22' not in g or "versionName '1.21.0'" not in g:
    raise SystemExit('version patch failed')

# Every new manual position gets a risk-price anchor from the quote used to open it.
req("            entry: entry, stop: stop, target: target, margin: margin,\n            entryTime: quote.t, entryIndex: quote.index == null ? Infinity : quote.index,",
    "            entry: entry, stop: stop, target: target, margin: margin,\n            lastRiskBid: quote.bid, lastRiskAsk: quote.ask,\n            entryTime: quote.t, entryIndex: quote.index == null ? Infinity : quote.index,")

# When SL/TP are confirmed, re-arm crossing detection from the current market price.
req("            position.stop = draft.stop; position.target = draft.target;",
    "            position.stop = draft.stop; position.target = draft.target;\n            if (quote) { position.lastRiskBid = quote.bid; position.lastRiskAsk = quote.ask; }")

# Modal edit also re-arms protection from the current quote.
req("            editPosition.stop = editStop;\n            editPosition.target = editTarget;",
    "            editPosition.stop = editStop;\n            editPosition.target = editTarget;\n            var editQuoteNow = currentManualQuote();\n            if (editQuoteNow) { editPosition.lastRiskBid = editQuoteNow.bid; editPosition.lastRiskAsk = editQuoteNow.ask; }")

# Never execute market SL/TP from a cached/UI quote. Live mode must explicitly mark a fresh tick.
req("        options = options || {};\n        if (!state.account || !quote) return;",
    "        options = options || {};\n        if (!state.account || !quote) return;\n        if (!replayCandle && state.chartMode === 'market' && !options.freshLive) {\n            recalculateManualAccount(quote);\n            refreshManualOverlay(false);\n            updatePositionQuickPanel();\n            if (state.page === 'trades' || state.page === 'history') renderRobotTerminal();\n            return;\n        }")

# Replace the SL/TP hit loop: live trading closes only when price CROSSES a level.
pat=re.compile(r"        var positions = \(state\.account\.manualPositions \|\| \[\]\)\.slice\(\);\n        positions\.forEach\(function \(position\) \{.*?\n        \}\);\n        recalculateManualAccount\(quote\);",re.S)
m=pat.search(js)
if not m:
    raise SystemExit('positions loop not found')
new_loop="""        var positions = (state.account.manualPositions || []).slice();
        positions.forEach(function (position) {
            var stopHit = false, targetHit = false;
            if (replayCandle) {
                if (position.side === 'buy') {
                    stopHit = position.stop != null && bidLow <= position.stop;
                    targetHit = position.target != null && bidHigh >= position.target;
                } else {
                    stopHit = position.stop != null && askHigh >= position.stop;
                    targetHit = position.target != null && askLow <= position.target;
                }
            } else {
                var prevBid = isFinite(Number(position.lastRiskBid)) ? Number(position.lastRiskBid) : quote.bid;
                var prevAsk = isFinite(Number(position.lastRiskAsk)) ? Number(position.lastRiskAsk) : quote.ask;
                if (position.side === 'buy') {
                    stopHit = position.stop != null && prevBid > position.stop && quote.bid <= position.stop;
                    targetHit = position.target != null && prevBid < position.target && quote.bid >= position.target;
                } else {
                    stopHit = position.stop != null && prevAsk < position.stop && quote.ask >= position.stop;
                    targetHit = position.target != null && prevAsk > position.target && quote.ask <= position.target;
                }
                position.lastRiskBid = quote.bid;
                position.lastRiskAsk = quote.ask;
            }
            if (stopHit) { closeManualPosition(position.ticket, 'Stop Loss', quote, position.stop, { deferUi:true }); changed = true; }
            else if (targetHit) { closeManualPosition(position.ticket, 'Take Profit', quote, position.target, { deferUi:true }); changed = true; }
        });
        recalculateManualAccount(quote);"""
js=js[:m.start()]+new_loop+js[m.end():]

# Do not force-close positions via Stop Out until the margin engine is stable.
pat=re.compile(r"\n        var marginLevel = state\.account\.margin > 0 \? state\.account\.equity / state\.account\.margin \* 100 : Infinity;\n        if \(marginLevel <= state\.account\.stopOutLevel && state\.account\.manualPositions\.length\) \{.*?\n        \}",re.S)
m=pat.search(js)
if not m:
    raise SystemExit('stopout block not found')
replacement="""
        var marginLevel = state.account.margin > 0 ? state.account.equity / state.account.margin * 100 : Infinity;
        state.account.marginLevel = marginLevel;
        state.account.marginWarning = marginLevel <= state.account.stopOutLevel ? 'warning' : '';"""
js=js[:m.start()]+replacement+js[m.end():]

# Stable chart while dragging SL/TP: don't reset zoom/offset on every finger move.
js=js.replace("        if (liveChart) { liveChart.priceScale = 1; liveChart.priceOffset = 0; }\n        if (replayChart) { replayChart.priceScale = 1; replayChart.priceOffset = 0; }\n","")

# Entry line label = floating P/L only, no USD suffix and never a dark background.
js=js.replace("var pnlLabel = (pnl >= 0 ? '+' : '') + pnl.toFixed(2) + ' USD';","var pnlLabel = (pnl >= 0 ? '+' : '') + pnl.toFixed(2);")
js=js.replace("                if (!plainLabel) {\n                    ctx.fillStyle = 'rgba(7,17,31,.90)';","                if (!plainLabel && kind !== 'entry') {\n                    ctx.fillStyle = 'rgba(7,17,31,.90)';")
js=js.replace("ctx.font = plainLabel ? 'bold 9px sans-serif' : 'bold 7px sans-serif';","ctx.font = (plainLabel || kind === 'entry') ? 'bold 11px sans-serif' : 'bold 7px sans-serif';")

# Update legacy tests to the new version when present.
for p in [root/'tools/test-manual-trading.js',root/'tools/test-meta-ui-v118.js',root/'tools/test-v120.js']:
    if p.exists():
        t=p.read_text()
        for old in ["versionName '1.18.0'","versionName '1.19.0'","versionName '1.20.0'"]:
            t=t.replace(old,"versionName '1.21.0'")
        for old in ['versionCode 19','versionCode 20','versionCode 21']:
            t=t.replace(old,'versionCode 22')
        for old in ["version:'1.18.0'","version:'1.19.0'","version:'1.20.0'"]:
            t=t.replace(old,"version:'1.21.0'")
        p.write_text(t)

# New regression test: no auto Stop Out, live SL/TP requires crossing, P/L label is plain.
test=root/'tools/test-v121.js'
test.write_text(r'''const fs=require('fs'),path=require('path');
const js=fs.readFileSync(path.resolve(__dirname,'../app/src/main/assets/app.js'),'utf8');
const grad=fs.readFileSync(path.resolve(__dirname,'../app/build.gradle'),'utf8');
for(const t of ["versionName '1.21.0'",'versionCode 22']) if(!grad.includes(t)) throw Error('version '+t);
for(const t of ['prevBid > position.stop && quote.bid <= position.stop','prevBid < position.target && quote.bid >= position.target','prevAsk < position.stop && quote.ask >= position.stop','prevAsk > position.target && quote.ask <= position.target','!options.freshLive','position.lastRiskBid = quote.bid','state.account.marginWarning']) if(!js.includes(t)) throw Error('missing '+t);
if(js.includes("closeManualPosition(worst.ticket, 'Stop Out'")) throw Error('automatic Stop Out still closes positions');
if(js.includes("priceScale = 1; liveChart.priceOffset = 0")) throw Error('drag still resets live chart scale');
if(!js.includes("var pnlLabel = (pnl >= 0 ? '+' : '') + pnl.toFixed(2);")) throw Error('P/L only label missing');
if(!js.includes("if (!plainLabel && kind !== 'entry')")) throw Error('entry label can still get black background');
console.log(JSON.stringify({crossingOnly:true,noAutoStopOut:true,stableDrag:true,plainPnlOnly:true,version:'1.21.0'}));
''')

app.write_text(js)
grad.write_text(g)
print('v1.21 patch applied')
