#!/usr/bin/bash

root=$(git rev-parse --show-toplevel)
for f in $(ls "$root/flags/")
do
	convert "$root/flags/$f" -crop 12x12+0+0 -alpha on \( +clone -channel a -fx 0 \) +swap "$root/scripts/mask.png" -composite "$root/src/main/resources/dev/dkvl/womutils/$f"
done

#optipng -o7 -strip all -silent "$root/src/main/resources/*"
