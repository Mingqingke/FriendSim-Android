(function(){
  var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if(!reduce && 'IntersectionObserver' in window){
    var io = new IntersectionObserver(function(es){
      es.forEach(function(e){ if(e.isIntersecting){ e.target.classList.add('in'); io.unobserve(e.target); } });
    }, {rootMargin:'0px 0px -10% 0px', threshold:0.05});
    document.querySelectorAll('.card, section h2, section .sub').forEach(function(el, i){
      el.classList.add('reveal');
      el.style.transitionDelay = Math.min(i % 4 * 60, 180) + 'ms';
      io.observe(el);
    });
  }
  // 版本号取自仓库里的 update.json，读不到就保留静态值
  fetch('update.json', {cache:'no-store'}).then(function(r){ return r.ok ? r.json() : null; }).then(function(d){
    if(!d) return;
    var el = document.getElementById('ver');
    if(el && (d.display || d.version)) el.textContent = d.display || String(d.version).replace(/^v/,'');
  }).catch(function(){});
})();
