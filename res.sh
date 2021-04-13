#!/bin/bash
cd $(dirname $0)/out

ln -sf res/{log4j2.xml,sql} .
ln -sf $PWD/res/com/gameroom/ui/icon com/gameroom/ui/icon 
