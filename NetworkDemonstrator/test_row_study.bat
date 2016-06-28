set "THETA=0"
set "ROW=0.01 0.05 0.1 1.0"
set "MAX_STATIONS=1"

 for %%i in (%THETA%) do (
 	for %%j in (%ROW%) do (
  		java -jar target/NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i %%j %MAX_STATIONS%
	)
)