#!/bin/sh

RNDFILE=$TMPDIR/$RANDOM

NEWKEY=`curl -s -k --data-binary @"$1" --header "Content-Type:image/jpeg" --header "X-Id-Prefix:Test_" http://localhost:8080`

echo "Uploaded file $1 as key=$NEWKEY"

curl -s http://localhost:8080/$NEWKEY >$RNDFILE

echo "Retrieved it back to $RNDFILE"

if diff $RNDFILE $1; then
    echo "Original file and retrieved file identical"
else
    echo "ERROR: Original file and retrieved file NOT identical"
fi

rm -f $RNDFILE

echo "Tmp file deleted"