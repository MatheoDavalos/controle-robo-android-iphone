#pragma once

#include <Arduino.h>

// Pagina autocontida: funciona sem internet e pode ser adicionada a Tela de Inicio.
static const char CONTROL_PAGE[] PROGMEM = R"HTML(
<!doctype html><html lang="pt-BR"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover">
<meta name="apple-mobile-web-app-capable" content="yes"><meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<title>Controle do Robô</title><style>
:root{color-scheme:dark;--bg:#070b12;--panel:#101827;--line:#29364b;--blue:#35a7ff;--red:#ff4d61;--muted:#91a0b7}*{box-sizing:border-box;touch-action:none;-webkit-user-select:none;user-select:none}
html,body{margin:0;width:100%;height:100%;overflow:hidden;background:radial-gradient(circle at 50% -20%,#19304d,var(--bg) 55%);font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;color:#fff}
body{padding:max(10px,env(safe-area-inset-top)) max(12px,env(safe-area-inset-right)) max(10px,env(safe-area-inset-bottom)) max(12px,env(safe-area-inset-left));display:grid;grid-template-rows:auto 1fr;gap:10px}
header{display:flex;align-items:center;justify-content:space-between;gap:12px}.brand{font-weight:800;letter-spacing:.04em}.status{font-size:13px;color:var(--muted);display:flex;align-items:center;gap:7px}.dot{width:9px;height:9px;border-radius:50%;background:#f0b429;box-shadow:0 0 10px currentColor}.dot.ok{background:#36d17c}.dot.busy{background:var(--red)}
main{min-height:0;display:grid;grid-template-columns:1fr auto 1fr;align-items:center;gap:clamp(12px,4vw,54px)}.control{text-align:center;min-width:0}.label{font-size:12px;font-weight:700;color:var(--muted);letter-spacing:.14em;margin-bottom:7px}
.pad{position:relative;margin:auto;width:min(36vw,34vh,280px);height:min(36vw,34vh,280px);min-width:150px;min-height:150px;border-radius:50%;background:linear-gradient(145deg,#18253a,#0d1421);border:2px solid var(--line);box-shadow:inset 0 0 35px #0008,0 12px 30px #0008}.pad:before,.pad:after{content:"";position:absolute;background:#2a3a52;left:12%;right:12%;height:1px;top:50%}.pad:after{top:12%;bottom:12%;left:50%;right:auto;width:1px;height:auto}
.knob{position:absolute;width:36%;height:36%;left:32%;top:32%;border-radius:50%;background:linear-gradient(145deg,#52baff,#1679c8);border:3px solid #79caff;box-shadow:0 8px 22px #000b,0 0 22px #269cff55;transform:translate(0,0)}
.stop{width:92px;height:92px;border-radius:50%;border:4px solid #ff8290;background:linear-gradient(#fa4055,#b80e25);color:#fff;font-size:14px;font-weight:900;letter-spacing:.08em;box-shadow:0 10px 26px #000b,0 0 22px #ff314955}.stop:active{transform:scale(.94)}
@media(max-width:680px) and (orientation:portrait){main{grid-template-columns:1fr 1fr;grid-template-rows:1fr auto}.stop{grid-column:1/-1;grid-row:2;width:76px;height:76px}.pad{width:min(42vw,31vh);height:min(42vw,31vh);min-width:130px;min-height:130px}}
</style></head><body><header><div class="brand">CONTROLE DO ROBÔ</div><div class="status"><span id="dot" class="dot"></span><span id="status">Pronto</span></div></header>
<main><section class="control"><div class="label">MOVIMENTO</div><div id="move" class="pad"><div class="knob"></div></div></section><button id="stop" class="stop">PARAR</button><section class="control"><div class="label">DIRECAO</div><div id="steer" class="pad"><div class="knob"></div></div></section></main>
<script>
(()=>{'use strict';let movement=0,steering=0,armed=false,busy=false,lastByte=-1;
const $=id=>document.getElementById(id),status=$('status'),dot=$('dot');
function level(v){return v<=0?0:v<=1/3?1:v<=2/3?2:3}function norm(v){v=Math.max(-1,Math.min(1,v));let a=Math.abs(v);return a<=.08?0:(a-.08)/.92*Math.sign(v)}
function command(){let m=Math.abs(norm(movement)),s=norm(steering);if(!m)return 0;let right=Math.max(1,level(Math.max(0,Math.min(1,m*(1-s))))),left=Math.max(1,level(Math.max(0,Math.min(1,m*(1+s))))),dir=movement>0?0x90:0;return dir|(left<<5)|(right<<2)}
async function send(force=false){if(!armed&&!force||busy)return;let value=force?0:command();if(!force&&value===lastByte&&document.hidden)return;busy=true;try{let r=await fetch('/api/command?v='+value,{cache:'no-store'}),j=await r.json();if(!r.ok){status.textContent='Em uso por '+j.owner;dot.className='dot busy'}else{lastByte=value;status.textContent=value?'Controlando · 0x'+value.toString(16).padStart(2,'0').toUpperCase():'Parado';dot.className='dot ok'}}catch(e){status.textContent='Sem conexao com o robo';dot.className='dot busy'}finally{busy=false}}
function joystick(id,axis){let pad=$(id),knob=pad.firstElementChild,pointer=null;function update(e){let r=pad.getBoundingClientRect(),x=(e.clientX-r.left-r.width/2)/(r.width*.32),y=(e.clientY-r.top-r.height/2)/(r.height*.32),d=Math.hypot(x,y);if(d>1){x/=d;y/=d}knob.style.transform=`translate(${x*88}%,${y*88}%)`;if(axis==='y')movement=-y;else steering=x;armed=true;send()}
pad.addEventListener('pointerdown',e=>{pointer=e.pointerId;pad.setPointerCapture(pointer);update(e)});pad.addEventListener('pointermove',e=>{if(e.pointerId===pointer)update(e)});function end(e){if(e.pointerId!==pointer)return;pointer=null;knob.style.transform='translate(0,0)';if(axis==='y')movement=0;else steering=0;send()}pad.addEventListener('pointerup',end);pad.addEventListener('pointercancel',end)}
joystick('move','y');joystick('steer','x');$('stop').addEventListener('pointerdown',()=>{movement=steering=0;armed=true;document.querySelectorAll('.knob').forEach(k=>k.style.transform='translate(0,0)');send(true)});
setInterval(()=>{if(armed)send()},180);document.addEventListener('visibilitychange',()=>{if(document.hidden){navigator.sendBeacon('/api/command?v=0');armed=false}});addEventListener('pagehide',()=>navigator.sendBeacon('/api/command?v=0'));fetch('/api/status').then(r=>r.json()).then(j=>{status.textContent='Pronto · '+j.ip;dot.className='dot ok'}).catch(()=>{});
})();
</script></body></html>
)HTML";
