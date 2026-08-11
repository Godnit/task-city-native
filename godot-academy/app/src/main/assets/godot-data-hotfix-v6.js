/* Godot Academy v6 — curriculum integrity hotfix */
(function(){
  const collision=DATA.lessons.find(x=>x.title==='CollisionShape3D والفيزياء');
  if(collision){
    collision.code='# CollisionShape3D يحدد شكل التصادم الفيزيائي\n# استخدم CapsuleShape3D للشخصية عندما يناسب شكلها';
    collision.q='ما الذي يحدد جسم التصادم للشخصية؟';
    collision.opts=['CollisionShape3D','Material','Theme'];
    collision.ans=0;
    collision.diagramV4='physics3d';
  }

  const firstScript=DATA.lessons.find(x=>x.title==='أول GDScript تفهمه');
  if(firstScript){
    firstScript.code='var count = 0\n\nfunc increase_count():\n    count += 1\n    $Label.text = str(count)';
  }
})();
