/* Godot Academy v6 — curriculum integrity hotfix */
(function(){
  const l=DATA.lessons.find(x=>x.title==='CollisionShape3D والفيزياء');
  if(l){
    l.code='# CollisionShape3D يحدد شكل التصادم الفيزيائي\n# استخدم CapsuleShape3D للشخصية عندما يناسب شكلها';
    l.q='ما الذي يحدد جسم التصادم للشخصية؟';
    l.opts=['CollisionShape3D','Material','Theme'];
    l.ans=0;
    l.diagramV4='physics3d';
  }
})();
