Team Memembers: Nikhil Tiware UFID : 91507670  emailID: nikhiltiware@ufl.edu
				Parikshit Tiwari UFID : 79218564 emailID: pariksh1tatiwari@ufl.edu 
				
				

--------------------------------------------------------------------------------------------				

How to execute the code?


1)Open terminal/command prompt on a your machine

2) Navigate to the folder location

eg Navigate to /home/Documents/project2

3)Execute the commands sbt compile

4)Use the run command as follows 

sbt "project project_name" "run numNodes topology algorithm failureParameter" 

Following is the list of arguements to be passed

project_name = project2-bonus
numNodes = The number nodes you want in the network is an integer value
Topology = full,line,3d,imp3d (Case insensitive)
Algorithm = gossip, push-sum (Case insensitive)
failureParameter = Is an integer value which determines how many nodes fail 

for example if you want to execute 27 nodes in 3d topology with pushsum algorithm type in 
sbt "project project2" "run 27 3d push-sum 9"

		
--------------------------------------------------------------------------------------------
		
working:

The failure model works by taking an input parameter from the user. 

The failure parameter then kills of those nodes whose ID value is a multiple 
of the failure parameter.

Thus higher the failure parameter lower the number of nodes will be killed.

A scheduler  is used to initiate the kill process.


--------------------------------------------------------------------------------------------
