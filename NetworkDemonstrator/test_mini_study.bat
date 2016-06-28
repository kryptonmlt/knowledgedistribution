set "THETA=0 0.001 0.01 0.1 5.0"
set "K=1 5 15"
set "MAX_STATIONS=36"

 for %%i in (%THETA%) do (
 	for %%j in (%K%) do (
  		java -jar target/NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i %%j %MAX_STATIONS%
	)
)