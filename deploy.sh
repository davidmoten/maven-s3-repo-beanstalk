#!/bin/bash
MODE=$1
if [ $# -eq 0 ]
  then
    echo "Usage: ./deploy.sh <mode>"
    exit 1
fi
mvn clean package aws:deployFileS3@file aws:deployCf@cf -Dmode=$MODE $1 $2 $3 $4 $5 $6 $7 $8
