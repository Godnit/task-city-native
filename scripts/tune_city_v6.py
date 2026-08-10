from pathlib import Path
p=Path('scripts/build_city_asset_v4.py')
s=p.read_text()
s=s.replace("width+1.10", "width+0.72")
s=s.replace("],2.45)", "],1.72)")
s=s.replace("],2.35)", "],1.68)")
s=s.replace("],2.25)", "],1.58)")
s=s.replace("scales=[0.80,0.76,0.82,0.78,0.80,0.75,0.81,0.77,0.78,0.79,0.76,0.80,0.82,0.77,0.79,0.76,0.81,0.78,0.80,0.75]",
            "scales=[1.02,0.98,1.05,1.00,1.03,0.97,1.04,0.99,1.00,1.02,0.98,1.03,1.05,0.99,1.02,0.98,1.04,1.00,1.03,0.97]")
s=s.replace("hedge(owner+'_HEDGE_A',x-1.55,z+1.65,2.6,0)", "hedge(owner+'_HEDGE_A',x-1.40,z+1.55,1.85,0)")
s=s.replace("hedge(owner+'_HEDGE_B',x+1.55,z+1.65,2.0,0)", "hedge(owner+'_HEDGE_B',x+1.40,z+1.55,1.45,0)")
s=s.replace("fence(owner+'_FENCE_A',x,z-1.65,3.8,0)", "fence(owner+'_FENCE_A',x,z-1.55,2.85,0)")
s=s.replace("octa(owner+'_GARDEN_B','shrub',x+1.25,0.30,z-1.05,0.38,0.30,0.38)",
'''octa(owner+'_GARDEN_B','shrub',x+1.25,0.30,z-1.05,0.38,0.30,0.38)
    tree(owner+'_YARD_TREE',x+1.55,z+1.05,0.52)''')
s=s.replace("tree(f'SCENERY_TREE_{i:02d}',x,z,0.82+(i%3)*0.09)", "tree(f'SCENERY_TREE_{i:02d}',x,z,1.02+(i%3)*0.09)")
p.write_text(s)
print('TASK_CITY_V6_PROPORTIONS_OK')
