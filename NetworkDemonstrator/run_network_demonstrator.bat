set "THETA=0.01"
for %%i in (%THETA%) do (
java -jar target\NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i
)