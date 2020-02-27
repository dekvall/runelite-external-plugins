import json
import re
from pprint import pprint
from shutil import copyfile
from markdown_table import Table, Column
from collections import OrderedDict

blacklist = ['diamond', 'square', 'clock', 'stuck_out_tongue', 'circle', 'u5', 'u6', 'u7']

valid = re.compile("^[0-9a-f\-]+$")
with open("emojis.json") as f:
	data = json.load(f)
	clean = {}
	count = 0
	for p in data['emojis']:
		if p['name'] and p['shortname'] and valid.match(p['unicode']) and all(ll not in p['shortname'] for ll in blacklist) and not p['shortname'].endswith('2:'):
			clean[p['name']] = [p['shortname'], p['unicode']]

clean = OrderedDict(sorted(clean.items()))

new_world_order = OrderedDict()

with open('thing.java', 'w') as f:
	for key, value in OrderedDict(sorted(clean.items())).items():
		shortcode, utf = value
		removeSpecialChars = key.translate ({ord(c): "" for c in "!@#$%^&*()[]\{\};:,./<>?\|`\'~-=+"})
		upper = removeSpecialChars.upper()
		upper = upper.replace(" ", "_")

		handled_shortcode = shortcode.replace(':', '')
		if handled_shortcode.startswith('flag-'):
			fl, country = handled_shortcode.split("-")
			handled_shortcode = " ".join([country, fl])
		elif handled_shortcode.startswith('flag_'):
			fl, country = handled_shortcode.split("_")
			handled_shortcode = " ".join([country, fl])

		handled_shortcode = handled_shortcode.replace("_", " ")
		new_world_order[key] = [handled_shortcode, utf]
		f.write(upper+'('+'\"'+handled_shortcode+'\"'+', \"'+utf+'\"),\n')

with open('table.md', 'w') as f:
	header = "|Emoji|Name|Trigger|\n"
	sep = "|---|:---|:---|\n"
	f.write(header+sep)
	for key, value in new_world_order.items():
		shortcode, codepoint = value
		line = "|"+"|".join([f"![{key}]({'src/main/resources/dekvall/emojimadness/'+codepoint+'.png'})", " ".join(w[0].upper()+w[1:] for w in key.split('_')), f'`{shortcode}`'])+"|\n"
		f.write(line)

# for key, value in OrderedDict(sorted(clean.items())).items():
# 	## JAPAN and TURKEY have duplicates
# 	shortname, filename = value
# 	try:
# 		copyfile('runelite-emoji/'+filename+".png", 'resources/'+filename+".png")
# 	except:
# 		print(filename)
# 		print("failed to copy", shortname)
# 		copyfile('runelite-emoji/'+filename[2:]+".png", 'resources/'+filename+".png")



pprint(clean)

print(len(clean))
