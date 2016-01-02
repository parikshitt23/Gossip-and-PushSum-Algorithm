Team Memembers: Nikhil Tiware UFID : 91507670  emailID: nikhiltiware@ufl.edu
				Parikshit Tiwari UFID : 79218564 emailID: pariksh1tatiwari@ufl.edu 
				
				

--------------------------------------------------------------------------------------------				

How to execute the code?


1)Open terminal/command prompt on a your machine

2) Navigate to the folder location

e.g Navigate to /home/Documents/project2

3)Execute the commands sbt compile

4)Use the run command as follows 

sbt "project project_name" "run numNodes topology algorithm" 

project_name = project2
numNodes = The number nodes you want in the network is an integer value
Topology = full,line,3d,imp3d (Case insensitive)
Algorithm = gossip, push-sum (Case insensitive)

for example if you want to execute 27 nodes in 3d topology with pushsum algorithm type in 
sbt "project project2" "run 27 3d pushsum"

		
--------------------------------------------------------------------------------------------
		
working:

Gossip and PushSum algorithms have been implemented using  Akka actors. 

Using multi-dimensional arrays we have created the following topologies

1)Full Topology
2)Imperfect 3D Topology
3)Perfect 3D Topology
4)Line Topology

Gossip Algorithm: A node in gossip implementation stops transmitting once it has heard the rumour 10 times. 
We have used Akka Schedulers so that when a node hears a rumour for the first time, it continues transmitting the rumour randomly to its neighbours at 5 millisecond intervals, till it receives a rumour 10 times.

PushSum Implementation: A node in a PushSum implementation stops transmitting if the ratio of s/w doesn’t change by 10 to the power -10 in three rounds.
Here as well have used Akka schedulers in a similar manner to Gossip.


--------------------------------------------------------------------------------------------
Largest network achieved for various topologies

Gossip Algorithm
Full Topology Largest network: 729 nodes Convergence Time : 421 milliseconds (The number of nodes can be increased however we get java.langOutOfMemory error. Here we are lmited by the system heap size)
Imperfect 3D Topology Largest network: 64,000 nodes Convergence Time : 6372 milliseconds
Perfecterfect 3D Topology Largest network: 64,000 nodes Convergence Time : 11463 milliseconds
Line Topology Largest network: 512 nodes Convergence Time : 1182 milliseconds

PushSum Algorithm
Full Topology Largest network: 512 nodes Convergence Time : 3582 milliseconds
Imperfect 3D Topology Largest network: 729 nodes Convergence Time : 6196 milliseconds
Perfecterfect 3D Topology Largest network: 512 nodes Convergence Time : 4529 milliseconds
Line Topology Largest network: 25 nodes Convergence Time : 1072 milliseconds
--------------------------------------------------------------------------------------------