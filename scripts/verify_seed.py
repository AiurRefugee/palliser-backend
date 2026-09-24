#!/usr/bin/env python3
"""Dependency-free verification of captured catalog data and SQLite schema."""
import json
import sqlite3
from pathlib import Path

root=Path(__file__).resolve().parents[1]
data=json.loads((root/'src/main/resources/seed/catalog.json').read_text())
db=sqlite3.connect(':memory:')
db.executescript((root/'src/main/resources/schema-sqlite.sql').read_text())
for c in data['categories']:
    db.execute('INSERT INTO catalog_category VALUES (?,?,?,?,?)',
               (c['uid'],c['name'],c['path'],c['parentUid'],c['active']))
for p in data['products']:
    db.execute('INSERT INTO catalog_product VALUES (?,?,?,?,?,?)',
               (p['slug'],p['name'],p.get('sku'),p.get('smallImage'),p['detailAvailable'],p.get('seriesId')))
    for path in p['routes']:
        category=db.execute('SELECT uid FROM catalog_category WHERE path=?',(path.rsplit('/',1)[0],)).fetchone()
        assert category,path
        db.execute('INSERT INTO catalog_route VALUES (?,?,?)',(path,p['slug'],category[0]))
for slug,detail in data['productDetails'].items():
    db.execute('INSERT INTO catalog_product_detail VALUES (?,?)',(slug,json.dumps(detail)))
for sid,series in data['series'].items():
    db.execute('INSERT INTO catalog_series VALUES (?,?)',(sid,json.dumps(series)))
assert db.execute('PRAGMA foreign_key_check').fetchall()==[]
assert len(data['categories'])==20
assert len(data['products'])==351
assert len(data['productDetails'])==15
assert len(data['series'])==13
assert db.execute('SELECT COUNT(*) FROM catalog_route').fetchone()[0]==397
assert db.execute('SELECT COUNT(DISTINCT p.slug) FROM catalog_product p JOIN catalog_route r ON r.product_slug=p.slug JOIN catalog_category c ON c.uid=r.category_uid WHERE c.active=1').fetchone()[0]==349
for path,expected in [('/shop/living-room/sectionals',60),('/shop/bedroom',42),('/shop/home-accents',49)]:
    sql='''WITH RECURSIVE subtree(uid) AS (SELECT uid FROM catalog_category WHERE path=? UNION ALL SELECT c.uid FROM catalog_category c JOIN subtree s ON c.parent_uid=s.uid)
    SELECT COUNT(DISTINCT p.slug) FROM catalog_product p JOIN catalog_route r ON r.product_slug=p.slug JOIN catalog_category c ON c.uid=r.category_uid WHERE c.active=1 AND c.uid IN (SELECT uid FROM subtree)'''
    actual=db.execute(sql,(path,)).fetchone()[0]
    assert actual==expected,(path,actual,expected)
print('Seed/schema checks passed: 20 categories, 351 products, 397 routes, 15 details, 13 series; active Shop 349.')
