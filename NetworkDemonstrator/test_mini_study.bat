set "THETA=0 0.001 0.01 0.1 1.0 5.0"
set "MAX_STATIONS=36"

for %%i in (%THETA%) do (
	java -jar target/NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i %MAX_STATIONS%
)