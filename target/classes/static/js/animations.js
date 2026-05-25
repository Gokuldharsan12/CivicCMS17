/* ═══════════════════════════════════════════════════════════════
   CivicCMS — animations.js v3  |  Light Theme
   Scroll reveal + count-up + navbar scroll
   ═══════════════════════════════════════════════════════════════ */
(function(){

  /* Tag cards for scroll reveal */
  function tagReveal(){
    document.querySelectorAll('.card,.chart-card').forEach(el=>{
      if(!el.classList.contains('reveal')) el.classList.add('reveal');
    });
  }

  /* Intersection Observer — adds .in */
  function initReveal(){
    const all = document.querySelectorAll('.reveal,.how-step,.kpi-card');
    if(!all.length) return;
    const io = new IntersectionObserver(entries=>{
      entries.forEach(e=>{
        if(e.isIntersecting){ e.target.classList.add('in'); io.unobserve(e.target); }
      });
    },{threshold:.1, rootMargin:'0px 0px -30px 0px'});
    all.forEach(el=>io.observe(el));
  }

  /* Count-up for .kpi-value numbers */
  function initCountUp(){
    const els = document.querySelectorAll('.kpi-value');
    if(!els.length) return;
    const io = new IntersectionObserver(entries=>{
      entries.forEach(e=>{
        if(!e.isIntersecting) return;
        const el = e.target;
        const raw = el.textContent.trim();
        const num = parseFloat(raw);
        if(isNaN(num)) return;
        io.unobserve(el);
        const dec = raw.includes('.');
        const dur = 1100, t0 = performance.now();
        function tick(t){
          const p = Math.min((t-t0)/dur,1);
          const ease = 1-Math.pow(1-p,3);
          el.textContent = dec ? (num*ease).toFixed(1) : Math.round(num*ease).toString();
          if(p<1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
      });
    },{threshold:.5});
    els.forEach(el=>io.observe(el));
  }

  /* Navbar shadow on scroll */
  function initNavbar(){
    const nav = document.querySelector('.navbar');
    if(!nav) return;
    window.addEventListener('scroll',()=>{
      nav.classList.toggle('scrolled', window.scrollY>8);
    },{passive:true});
  }

  /* Stagger login form fields */
  function initLoginStagger(){
    document.querySelectorAll('.login-card .form-group, .login-card .btn, .login-card>div')
      .forEach((el,i)=>{
        el.style.cssText = `opacity:0;transform:translateY(12px);
          transition:opacity .35s cubic-bezier(.22,1,.36,1) ${.3+i*.06}s,
          transform .35s cubic-bezier(.22,1,.36,1) ${.3+i*.06}s`;
        setTimeout(()=>{ el.style.opacity='1'; el.style.transform='none'; }, 30);
      });
  }

  function init(){
    tagReveal();
    initReveal();
    initCountUp();
    initNavbar();
    initLoginStagger();
  }

  if(document.readyState==='loading')
    document.addEventListener('DOMContentLoaded', init);
  else init();
})();
