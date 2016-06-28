set "THETA=0 0.001 0.005 0.01 0.05 0.1 0.5 1.0 5.0"
set "K=1 2 5 10 15 25 50"
set "MAX_STATIONS=1"

 for %%i in (%THETA%) do (
 	for %%j in (%K%) do (
  		java -jar target/NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i %%j %MAX_STATIONS%
	)
)