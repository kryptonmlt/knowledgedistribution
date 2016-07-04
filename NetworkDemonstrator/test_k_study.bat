set "THETA=1.0"
set "K=1 3 5 7 10 15 20 30"
set "MAX_STATIONS=36"

 for %%i in (%THETA%) do (
 	for %%j in (%K%) do (
  		java -jar target/NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i %%j %MAX_STATIONS%
	)
)