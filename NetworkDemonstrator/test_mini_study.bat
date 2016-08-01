set "THETA=0 0.001 0.01 0.1 0.5 1.0"
set "MAX_STATIONS=36"
set "GAMMA=0.5"

for %%i in (%THETA%) do (
	java -jar target/NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i %MAX_STATIONS% %GAMMA%
)