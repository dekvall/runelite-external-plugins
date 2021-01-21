#!/usr/bin/bash

[ ! -d "./out" ] && mkdir "./out"

for f in "$@"
do
	convert "$f" -alpha on \( +clone -channel a -fx 0 \) +swap mask.png -composite "out/$f"
done

optipng -o7 -strip all out/*
