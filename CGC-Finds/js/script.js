(function(){
'use strict';

/* ---------- data ---------- */
var CATS=[['electronics','Electronics'],['documents','Documents & ID cards'],['bags','Wallets & bags'],['keys','Keys'],['clothing','Clothing & accessories'],['jewellery','Jewellery & watches'],['books','Books & stationery'],['other','Other']];
var CAT={}; CATS.forEach(function(c){CAT[c[0]]=c[1];});
var KEY='cgcfinds.items.v1';

function iso(offsetDays){var d=new Date();d.setDate(d.getDate()-(offsetDays||0));var m=String(d.getMonth()+1).padStart(2,'0');var day=String(d.getDate()).padStart(2,'0');return d.getFullYear()+'-'+m+'-'+day;}
function sample(){
  var s=[
    ['lost','iPhone 13','electronics','Blue mobile phone with a cracked back cover','Central library',2,'98765 43210'],
    ['found','Smartphone','electronics','Blue phone, cracked cover, left on a reading table','Library reading hall',1,'Security desk, main gate'],
    ['lost','Black wallet','bags','Leather wallet with college ID card and some cash','Canteen',3,'rahul@example.com'],
    ['found','Purse','bags','Black leather purse containing an ID card','Canteen',1,'Canteen counter'],
    ['lost','House keys','keys','Bunch of 3 keys with a red keychain','Parking lot',4,'99887 76655'],
    ['found','Steel water bottle','other','Silver steel bottle, dented near the cap','Sports ground',1,'Sports office'],
    ['lost','Scientific calculator','books','Casio calculator with name sticker on the back','Block B, Room 204',5,'priya@example.com'],
    ['found','Grey umbrella','clothing','Large grey umbrella with wooden handle','Auditorium',2,'Reception']
  ];
  return s.map(function(r,i){return {id:i+1,type:r[0],name:r[1],category:r[2],description:r[3],place:r[4],date:iso(r[5]),contact:r[6],resolved:false,sample:true};});
}
var items=[];
function load(){
  var raw=null;
  try{raw=localStorage.getItem(KEY);}catch(e){}
  if(raw){try{var a=JSON.parse(raw);if(Array.isArray(a)){items=a;return;}}catch(e){}}
  items=sample();save();
}
function save(){try{localStorage.setItem(KEY,JSON.stringify(items));}catch(e){}}
function nextId(){return items.reduce(function(m,i){return Math.max(m,i.id);},0)+1;}
function byId(id){for(var i=0;i<items.length;i++){if(items[i].id===id)return items[i];}return null;}

/* ---------- keyword matching (same idea as the Java version) ---------- */
var STOP={};'a an the and or of in on at to for with my his her its our is was it this that has have had near by from very some one lost found item small big large'.split(' ').forEach(function(w){STOP[w]=1;});
var SYN={};
[['phone','mobile cellphone smartphone iphone phone'],['bag','bag backpack rucksack handbag satchel'],['wallet','wallet purse billfold'],['laptop','laptop macbook'],['earphone','earphone earbud headphone airpod headset'],['idcard','id identity aadhar aadhaar licence license'],['glasses','spectacle glass glasse eyeglass eyeglasse specs'],['watch','watch wristwatch'],['bottle','bottle flask'],['key','key keychain keyring']].forEach(function(p){p[1].split(' ').forEach(function(w){SYN[w]=p[0];});});
function kw(text){
  var out={};
  String(text).toLowerCase().split(/[^a-z0-9]+/).forEach(function(raw){
    if(!raw||STOP[raw])return;
    var w=raw;
    if(w.length>3&&w.charAt(w.length-1)==='s'&&w.slice(-2)!=='ss')w=w.slice(0,-1);
    if(SYN[w])w=SYN[w];
    if(!STOP[w]&&w.length>1)out[w]=1;
  });
  return Object.keys(out);
}
function suggest(t){
  var tw=kw(t.name+' '+t.description), tp=kw(t.place), res=[];
  items.forEach(function(o){
    if(o.type===t.type||o.resolved||o.id===t.id)return;
    var ow=kw(o.name+' '+o.description);
    var shared=tw.filter(function(w){return ow.indexOf(w)>-1;});
    if(!shared.length)return;
    var score=Math.round(2*shared.length/(tw.length+ow.length)*70);
    if(o.category===t.category)score+=20;
    var op=kw(o.place);
    if(tp.some(function(w){return op.indexOf(w)>-1;}))score+=10;
    score=Math.min(score,100);
    if(score>=30)res.push({item:o,score:score,shared:shared.sort()});
  });
  res.sort(function(a,b){return b.score-a.score;});
  return res;
}

/* ---------- helpers ---------- */
function esc(s){return String(s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
function no(it){return 'CGC-'+String(it.id).padStart(4,'0');}
function fmt(d){var x=new Date(d+'T00:00:00');return isNaN(x)?d:x.toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'});}
function $(id){return document.getElementById(id);}

/* ---------- browse ---------- */
var f={type:'all',cat:'all',q:'',done:false};
function renderChips(){
  var all=[['all','All categories']].concat(CATS);
  $('chips').innerHTML=all.map(function(c){return '<button type="button" class="chip" data-cat="'+c[0]+'" aria-pressed="'+(f.cat===c[0])+'">'+esc(c[1])+'</button>';}).join('');
}
function cardHTML(it){
  var m=suggest(it).length;
  var label=m?m+' possible match'+(m>1?'es':''):'Check matches';
  return '<article class="ticket '+(it.resolved?'done':'')+'">'
   +'<div class="stub"><span class="no">'+no(it)+(it.sample?'<span class="ex">Example</span>':'')+'</span><span class="badge '+it.type+'">'+(it.type==='lost'?'Lost':'Found')+'</span></div>'
   +'<div class="tbody"><h3>'+esc(it.name)+'</h3><p class="cat">'+esc(CAT[it.category]||'Other')+'</p><p class="desc">'+esc(it.description)+'</p>'
   +'<dl class="meta"><div><dt>Place</dt><dd>'+esc(it.place)+'</dd></div><div><dt>Date</dt><dd>'+fmt(it.date)+'</dd></div><div><dt>Contact</dt><dd>'+esc(it.contact)+'</dd></div></dl></div>'
   +'<div class="actions"><button type="button" class="btn small" data-act="matches" data-id="'+it.id+'">'+label+'</button>'
   +(it.resolved?'<span class="reunited">Reunited</span>':'<button type="button" class="btn small ghost" data-act="resolve" data-id="'+it.id+'">Mark reunited</button>')
   +'</div></article>';
}
function renderBrowse(){
  var q=f.q.trim().toLowerCase(), qk=kw(q);
  var list=items.filter(function(it){
    if(!f.done&&it.resolved)return false;
    if(f.type!=='all'&&it.type!==f.type)return false;
    if(f.cat!=='all'&&it.category!==f.cat)return false;
    if(q){
      var text=(it.name+' '+it.description+' '+it.place).toLowerCase();
      if(text.indexOf(q)>-1)return true;
      var ik=kw(it.name+' '+it.description);
      return qk.some(function(w){return ik.indexOf(w)>-1;});
    }
    return true;
  }).sort(function(a,b){return b.id-a.id;});
  $('count').textContent=list.length+' report'+(list.length===1?'':'s')+' shown';
  $('grid').innerHTML=list.length?list.map(cardHTML).join(''):'<div class="empty" style="grid-column:1/-1">No reports match these filters. Try another category, or <a href="#report">report an item</a>.</div>';
  var hasSample=items.some(function(i){return i.sample;});
  $('foot').innerHTML='<span>Reports are saved in this browser.</span>'+(hasSample?'<button type="button" class="btn small ghost" id="rmSample">Remove example reports</button>':'');
  $('s-lost').textContent=items.filter(function(i){return i.type==='lost'&&!i.resolved;}).length;
  $('s-found').textContent=items.filter(function(i){return i.type==='found'&&!i.resolved;}).length;
  $('s-done').textContent=items.filter(function(i){return i.resolved;}).length;
}

/* ---------- matches panel ---------- */
function openMatches(it,title){
  var ms=suggest(it), want=it.type==='lost'?'found':'lost';
  $('panelTitle').textContent=title||('Matches for '+it.name);
  $('panelSub').textContent=no(it)+' · '+it.name+' · '+(it.type==='lost'?'Lost':'Found');
  var body;
  if(!ms.length){
    body='<div class="empty" style="margin-top:10px">No matching '+want+' reports yet. New reports are compared as they come in, so check this item again later.</div>';
  }else{
    body='<p class="sub" style="margin-top:0">Possible matching '+want+' reports, best first.</p>'+ms.map(function(m){
      var o=m.item;
      return '<div class="match"><div class="top"><h3>'+esc(o.name)+' <span class="no">'+no(o)+'</span></h3><span class="badge '+o.type+'">'+(o.type==='lost'?'Lost':'Found')+'</span></div>'
       +'<div class="score"><span>'+m.score+'% match</span><div class="bar2"><i style="width:'+m.score+'%"></i></div></div>'
       +'<div class="kws">'+m.shared.map(function(w){return '<span>'+esc(w)+'</span>';}).join('')+'</div>'
       +'<p class="desc" style="color:var(--muted)">'+esc(o.description)+'</p>'
       +'<dl class="meta"><div><dt>Place</dt><dd>'+esc(o.place)+'</dd></div><div><dt>Date</dt><dd>'+fmt(o.date)+'</dd></div><div><dt>Contact</dt><dd>'+esc(o.contact)+'</dd></div></dl>'
       +'<div><button type="button" class="btn small" data-act="both" data-id="'+it.id+'" data-other="'+o.id+'">Mark both reunited</button></div></div>';
    }).join('');
  }
  $('panelBody').innerHTML=body;
  $('overlay').hidden=false;
  $('panelClose').focus();
}
function closePanel(){$('overlay').hidden=true;}

var toastTimer;
function toast(msg){var t=$('toast');t.textContent=msg;t.hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(function(){t.hidden=true;},2600);}

/* ---------- report form ---------- */
var ftype='lost';
function setType(t){
  ftype=t;
  document.querySelectorAll('[data-ftype]').forEach(function(b){b.setAttribute('aria-pressed',String(b.getAttribute('data-ftype')===t));});
  $('placeLabel').textContent=t==='lost'?'Where did you lose it?':'Where did you find it?';
}
function resetForm(){$('form').reset();$('f-date').value=iso(0);$('f-date').max=iso(0);}

/* ---------- routing ---------- */
function route(){
  var h=location.hash.slice(1); if(h!=='report')h='browse';
  $('view-browse').hidden=h!=='browse'; $('view-report').hidden=h!=='report';
  $('nav-browse').setAttribute('aria-current',h==='browse'?'page':'false');
  $('nav-report').setAttribute('aria-current',h==='report'?'page':'false');
  window.scrollTo(0,0);
}

/* ---------- wiring ---------- */
function init(){
  load();
  $('f-cat').innerHTML=CATS.map(function(c){return '<option value="'+c[0]+'">'+esc(c[1])+'</option>';}).join('');
  resetForm(); setType('lost'); renderChips(); renderBrowse(); route();
  window.addEventListener('hashchange',route);

  document.addEventListener('click',function(e){
    var a=e.target.closest('a[data-type]'); if(a)setType(a.getAttribute('data-type'));
    var ft=e.target.closest('[data-ftype]'); if(ft)setType(ft.getAttribute('data-ftype'));
    var sg=e.target.closest('.seg button[data-type]'); if(sg){f.type=sg.getAttribute('data-type');document.querySelectorAll('.seg button[data-type]').forEach(function(b){b.setAttribute('aria-pressed',String(b===sg));});renderBrowse();}
    var ch=e.target.closest('.chip'); if(ch){f.cat=ch.getAttribute('data-cat');renderChips();renderBrowse();}
    var act=e.target.closest('[data-act]');
    if(act){
      var id=parseInt(act.getAttribute('data-id'),10), it=byId(id), kind=act.getAttribute('data-act');
      if(kind==='matches'&&it)openMatches(it);
      if(kind==='resolve'&&it){it.resolved=true;save();renderBrowse();toast('Marked as reunited');}
      if(kind==='both'&&it){var o=byId(parseInt(act.getAttribute('data-other'),10));it.resolved=true;if(o)o.resolved=true;save();closePanel();renderBrowse();toast('Both reports marked as reunited');}
    }
    if(e.target.id==='rmSample'){items=items.filter(function(i){return !i.sample;});save();renderBrowse();toast('Example reports removed');}
    if(e.target===$('overlay'))closePanel();
  });
  $('panelClose').addEventListener('click',closePanel);
  document.addEventListener('keydown',function(e){if(e.key==='Escape')closePanel();});
  $('q').addEventListener('input',function(e){f.q=e.target.value;renderBrowse();});
  $('showDone').addEventListener('change',function(e){f.done=e.target.checked;renderBrowse();});

  $('form').addEventListener('submit',function(e){
    e.preventDefault();
    var fd=new FormData($('form'));
    var it={id:nextId(),type:ftype,name:String(fd.get('name')).trim(),category:fd.get('category'),description:String(fd.get('description')).trim(),place:String(fd.get('place')).trim(),date:fd.get('date'),contact:String(fd.get('contact')).trim(),resolved:false,sample:false};
    items.push(it); save();
    resetForm(); setType('lost');
    f.type='all';f.cat='all';f.q='';$('q').value='';
    document.querySelectorAll('.seg button[data-type]').forEach(function(b){b.setAttribute('aria-pressed',String(b.getAttribute('data-type')==='all'));});
    renderChips(); renderBrowse();
    location.hash='browse'; route();
    openMatches(it,'Report saved as '+no(it));
  });
}
init();
})();
