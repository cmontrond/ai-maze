import java.awt.*;
import javax.swing.*;
import java.util.*;

public class Maze470_multipath
{
	private static class State
	{
		public int x;
		public int y;
		public State parent;
		public int mostRecentDirection;
		public String key;

		public State()
		{

		}

		public State(int x, int y)
		{
			this.x = x;
			this.y = y;
			this.key = this.x + ":" + this.y;
		}

		public State(int x, int y, State parent, int mostRecentDirection)
		{
			this.x = x;
			this.y = y;
			this.parent = parent;
			this.mostRecentDirection = mostRecentDirection;
			this.key = this.x + ":" + this.y;
		}
	}

	public static final int MWIDTH=30,MHEIGHT=30,BLOCK=20;
	public static boolean robotActive=true;
	public static final int SPEED=100;

	public static final int LEFT=4,RIGHT=8,UP=1,DOWN=2;

	//1=wall above, 2=wall below, 4=wall on left, 8=wall on right, 16=not included in maze yet
	static int[][] maze;
	static MazeComponent mazecomp;

	//current position of robot
	static int robotX=0,robotY=0;

	//true means that a "crumb" is shown in the room
	static boolean[][] crumbs;

	public static Stack<State> dfsStack;
	public static HashMap<String, Boolean> dfsVisited;

	public static Queue<State> aStarQueue;
	public static HashMap<String, Boolean> aStarVisited;

	public static void main(String[] args)
	{
		//maze a maze array and a crumb array
		maze=new int[MWIDTH][MHEIGHT];
		crumbs=new boolean[MWIDTH][MHEIGHT];
		//set each room to be surrounded by walls and not part of the maze
		for (int i=0; i<MWIDTH; i++) 
		{
			for (int j=0; j<MHEIGHT; j++)
			{
				maze[i][j]=31;
				crumbs[i][j]=false;
			}
		}

		//generate the maze
		makeMaze();

		//knock down up to 100 walls
		for(int i=0; i<100; i++)
		{
			int x=(int)(Math.random()*(MWIDTH-2));
			int y=(int)(Math.random()*MHEIGHT);
			if((maze[x][y]&RIGHT)!=0)
			{
				maze[x][y]^=RIGHT;
				maze[x+1][y]^=LEFT;
			}
		}

		JFrame f = new JFrame();
		f.setSize(MWIDTH*BLOCK+15,MHEIGHT*BLOCK+30);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setTitle("Maze!");
		mazecomp=new MazeComponent();
		f.add(mazecomp);
		f.setVisible(true);


		//have the robot wander around in its own thread
		if(robotActive)
        {
		    new Thread(new Runnable(){
			    public void run() {
					// doMazeRandomWalk();
					State finalState = doDfs();
					int[] directions = getDfsDirections(finalState);
					directions = reverseArray(directions, directions.length);
					doMazeGuided(directions);
			    }
		    }).start();
        }
	}

	public static void makeMaze()
	{
		int[] blockListX = new int[MWIDTH*MHEIGHT];
		int[] blockListY = new int[MWIDTH*MHEIGHT];
		int blocks=0;
		int x,y;

		//Choose random starting block and add it to maze
		x=(int)(Math.random()*(MWIDTH-2)+1);
		y=(int)(Math.random()*(MHEIGHT-2)+1);
		maze[x][y]^=16;

		//Add all adjacent blocks to blocklist
		if (x>0)
		{
			blockListX[blocks]=x-1;
			blockListY[blocks]=y;
			blocks++;
		}
		if (x<MWIDTH-1)
		{
			blockListX[blocks]=x+1;
			blockListY[blocks]=y;
			blocks++;
		}
		if (y>0)
		{
			blockListX[blocks]=x;
			blockListY[blocks]=y-1;
			blocks++;
		}
		if (y<MHEIGHT-1)
		{
			blockListX[blocks]=x;
			blockListY[blocks]=y+1;
			blocks++;
		}

		//approach:
		// start with a single room in maze and all neighbors of the room in the "blocklist"
		// choose a room that is not yet part of the maze but is adjacent to the maze
		// add it to the maze by breaking a wall
		// put all of its neighbors that aren't in the maze into the "blocklist"
		// repeat until everybody is in the maze
		while (blocks>0)
		{
			//choose a random block from blocklist
			int b = (int)(Math.random()*blocks);

			//find which block in the maze it is adjacent to
			//and remove that wall
			x=blockListX[b];
			y=blockListY[b];

			//get a list of all of its neighbors that aren't in the maze
			int[] dir=new int[4];
			int numdir=0;

			//left
			if (x>0 && (maze[x-1][y]&16)==0)
			{
				dir[numdir++]=0;
			}
			//right
			if (x<MWIDTH-1 && (maze[x+1][y]&16)==0)
			{
				dir[numdir++]=1;
			}
			//up
			if (y>0 && (maze[x][y-1]&16)==0)
			{
				dir[numdir++]=2;
			}
			//down
			if (y<MHEIGHT-1 && (maze[x][y+1]&16)==0)
			{
				dir[numdir++]=3;
			}

			//choose one at random
			int d = (int)(Math.random()*numdir);
			d=dir[d];

			//tear down the wall
			//left
			if (d==0)
			{
				maze[x][y]^=LEFT;
				maze[x-1][y]^=RIGHT;
			}
			//right
			else if (d==1)
			{
				maze[x][y]^=RIGHT;
				maze[x+1][y]^=LEFT;
			}
			//up
			else if (d==2)
			{
				maze[x][y]^=UP;
				maze[x][y-1]^=DOWN;
			}
			//down
			else if (d==3)
			{
				maze[x][y]^=DOWN;
				maze[x][y+1]^=UP;
			}

			//set that block as "in the maze"
			maze[x][y]^=16;

			//remove it from the block list
			for (int j=0; j<blocks; j++)
			{
				if ((maze[blockListX[j]][blockListY[j]]&16)==0)
				{
					for (int i=j; i<blocks-1; i++)
					{
						blockListX[i]=blockListX[i+1];
						blockListY[i]=blockListY[i+1];
					}
					blocks--;
					j=0;
				}
			}

			//put all adjacent blocks that aren't in the maze in the block list
			if (x>0 && (maze[x-1][y]&16)>0)
			{
				blockListX[blocks]=x-1;
				blockListY[blocks]=y;
				blocks++;
			}
			if (x<MWIDTH-1 && (maze[x+1][y]&16)>0)
			{
				blockListX[blocks]=x+1;
				blockListY[blocks]=y;
				blocks++;
			}
			if (y>0 && (maze[x][y-1]&16)>0)
			{
				blockListX[blocks]=x;
				blockListY[blocks]=y-1;
				blocks++;
			}
			if (y<MHEIGHT-1 && (maze[x][y+1]&16)>0)
			{
				blockListX[blocks]=x;
				blockListY[blocks]=y+1;
				blocks++;
			}
		}

		//remove top left and bottom right edges
//		maze[0][0]^=LEFT;    //commented out for now so that robot doesn't run out the entrance
		maze[MWIDTH-1][MHEIGHT-1]^=RIGHT;
	}

	//the robot will wander around aimlessly until it happens to stumble on the exit
	public static void doMazeRandomWalk()
	{
		int dir=RIGHT;

		while(robotX!=MWIDTH-1 || robotY!=MHEIGHT-1)
		{
			int x=robotX;
			int y=robotY;

			//choose a direction at random
			dir=new int[]{LEFT,RIGHT,UP,DOWN}[(int)(Math.random()*4)];
			//move the robot
			if((maze[x][y]&dir)==0)
			{
				if(dir==LEFT) robotX--;
				if(dir==RIGHT) robotX++;
				if(dir==UP) robotY--;
				if(dir==DOWN) robotY++;
			}

			//leave a crumb
			crumbs[x][y]=true;

			//repaint and pause momentarily
			mazecomp.repaint();
			try{ Thread.sleep(SPEED); } catch(Exception e) { }
		}
		System.out.println("Done");
	}

	public static void doMazeGuided(int[] directions) 
	{

		for (int direction : directions) {

			int x = robotX;
			int y = robotY;
			
			if((maze[x][y]&direction)==0) // Makes sure we don't hit a wall
			{
				if(direction==LEFT) robotX--;
				if(direction==RIGHT) robotX++;
				if(direction==UP) robotY--;
				if(direction==DOWN) robotY++;
			}

			//leave a crumb
			crumbs[x][y]=true;

			//repaint and pause momentarily
			mazecomp.repaint();
			try{ Thread.sleep(SPEED); } catch(Exception e) { }
		}

		System.out.println("Done: doMazeGuided!");
	}

	public static State doDfs()
	{
		dfsStack = new Stack<>();
		dfsVisited = new HashMap<>();

		State initialState = new State(robotX, robotY);

		dfsStack.push(initialState);

		while(!dfsStack.isEmpty())
		{
			State currentState = dfsStack.peek();
			dfsVisited.put(currentState.key, true);

			dfsStack.pop();

			Set<State> childStates = getDfsChildStates(currentState);

			for (State childState : childStates)
			{
				if (!dfsVisited.containsKey(childState.key)) {
					// If destination has been reached
					if (childState.x == (MWIDTH-1) && childState.y == (MHEIGHT-1))
					{
						return childState;
					}
					else
					{
						dfsStack.push(childState);
					}
				}
			}
		}

		return null;
	}

	public static State doAStar()
	{
		aStarQueue = new LinkedList<>();
		dfsVisited = new HashMap<>();

		State initialState = new State(robotX, robotY);

		aStarQueue.add(initialState);



		return new State();
	}

	public static int[] getDfsDirections(State state)
	{
		State currentState = state;
		ArrayList<Integer> directions = new ArrayList<>();

		while (currentState.parent != null)
		{
			directions.add(currentState.mostRecentDirection);
			currentState = currentState.parent;
		}

		return directions.stream().mapToInt(Integer::intValue).toArray();
	}

	public static int[] reverseArray(int[] intArray, int size) 
    { 
        int i, temp; 
        for (i = 0; i < size / 2; i++) { 
            temp = intArray[i]; 
            intArray[i] = intArray[size - i - 1]; 
            intArray[size - i - 1] = temp; 
		}
		
		return intArray;
    } 

	public static Set<State> getDfsChildStates(State parentState)
	{
		Set<State> result = new HashSet<State>();

		// TOP
		if ((maze[parentState.x][parentState.y]&UP)==0)
		{
			State newState = new State(parentState.x, parentState.y - 1, parentState, UP);
			result.add(newState);
		}

		// BOTTOM
		if ((maze[parentState.x][parentState.y]&DOWN)==0)
		{
			State newState = new State(parentState.x, parentState.y + 1, parentState, DOWN);
			result.add(newState);
		}

		// LEFT
		if ((maze[parentState.x][parentState.y]&LEFT)==0)
		{
			State newState = new State(parentState.x - 1, parentState.y, parentState, LEFT);
			result.add(newState);
		}

		// RIGHT
		if ((maze[parentState.x][parentState.y]&RIGHT)==0)
		{
			State newState = new State(parentState.x + 1, parentState.y, parentState, RIGHT);
			result.add(newState);
		}

		return result;
	}

	public static class MazeComponent extends JComponent
	{
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,MWIDTH*BLOCK,MHEIGHT*BLOCK);
			g.setColor(new Color(100,0,0));
			for (int x=0; x<MWIDTH; x++)
			{
				for (int y=0; y<MHEIGHT; y++)
				{
					if ((maze[x][y]&1)>0)
						g.drawLine(x*BLOCK,y*BLOCK,x*BLOCK+BLOCK,y*BLOCK);
					if ((maze[x][y]&2)>0)
						g.drawLine(x*BLOCK,y*BLOCK+BLOCK,x*BLOCK+BLOCK,y*BLOCK+BLOCK);
					if ((maze[x][y]&4)>0)
						g.drawLine(x*BLOCK,y*BLOCK,x*BLOCK,y*BLOCK+BLOCK);
					if ((maze[x][y]&8)>0)
						g.drawLine(x*BLOCK+BLOCK,y*BLOCK,x*BLOCK+BLOCK,y*BLOCK+BLOCK);
				}
			}

			if (robotActive)
			{
				g.setColor(Color.BLUE);
				for (int x=0; x<MWIDTH; x++)
				{
					for (int y=0; y<MHEIGHT; y++)
					{
						if (crumbs[x][y])
							g.fillRect(x*BLOCK+BLOCK/2-1,y*BLOCK+BLOCK/2-1,2,2);
					}
				}

				g.setColor(Color.GREEN);
				g.fillOval(robotX*BLOCK+1,robotY*BLOCK+1,BLOCK-2,BLOCK-2);
			}
		}
	}
}
