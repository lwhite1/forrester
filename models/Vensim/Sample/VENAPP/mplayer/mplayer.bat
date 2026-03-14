echo off
if .%1 == .mod goto mod
if .%1 == .setup goto setup
if .%1 == .cleanup goto cleanup
if .%1 == .north goto other
if .%1 == .south goto other
if .%1 == .east goto other
Echo "Use mplayer mod|setup|cleanup|north|south|east"
goto exit
:other
c:\vensim\vensim c:\vensim\models\sample\mplayer\%1\mplayer.vcd
goto exit
:mod 
c:\vensim\vensim c:\vensim\models\sample\mplayer\mplayer.vcd
goto exit
:setup
REM Set up the files to be used
md north
copy mplayer.vmf north
copy mplayer.vcd north
copy mplayer.vgd north
copy north.lst north
md south
copy mplayer.vmf south
copy mplayer.vcd south
copy mplayer.vgd south
copy south.lst south
md east
copy mplayer.vmf east
copy mplayer.vcd east
copy mplayer.vgd east
copy east.lst east
goto exit
:cleanup
del north
rd north
del south
rd south
del east
rd east
del *.vdf
del *.mrk
del *.tmp
:exit
