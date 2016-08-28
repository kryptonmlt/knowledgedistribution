set "THETA=0 0.1 1.0"
for %%i in (%THETA%) do (
java -jar NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i
)