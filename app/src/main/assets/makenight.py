from pathlib import Path
import re

p = Path(".")
for fp in p.glob("*.svg"):
	with fp.open() as f:
		t = f.read()
	t = re.sub(r'<svg(.*?) fill=".*?"(.*?)>', r'<svg\1\2>', t)
	t = re.sub(r'<svg(.*?)>', r'<svg\1 fill="white">', t)
	with open("night/" + fp.name, "w") as f:
		f.write(t)

