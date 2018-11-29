package performance.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

public class WangResearch {
	static String inputfile = "test_20180920_ClientIP";	// training_A.txt
	
	static int m_nUserSize = 16637;	// 14480
	static int m_nItemSize = 137016;	// 137016  303332
	static int m_nRequest = 269616;	// 269616  611968
	
	static int timethres = 2244475;	// 1~14 days
	static int thre_requestfreq = 86400;	
	
	static int thres_usercount = 10;	// 15;
	static int thres_filecount = 200;	// 150;
	static int thres_filecount_30 = 150;	// 65;
	
	static int request_file_num;
	
	public static class User {
		int ID = -1;
		int Fix_ID = -1;
		String Name;
		LinkedList<Integer> File = new LinkedList<Integer>();
		int count = 0;
		int count_duplicate = 0;
	}
	
	public static class File implements Comparable<File> {
		int ID = -1;
		int Fix_ID = -1;
		String Name;
		LinkedList<Integer> User = new LinkedList<Integer>();
		int count = 0;
		int count_duplicate = 0;
		
		public int compareTo(File arg) {
			if(this.count-arg.count!=0)
				return arg.count - this.count;
			else
				return 0;
			/*
			if(this.count_duplicate-arg.count_duplicate!=0)
				return arg.count_duplicate-this.count_duplicate;
			else
				return 0;
			*/
		}
	}	
	
	public static class Request {
		int ID = -1;
		int timestamp = -1;
		int UserID = -1;
		int VideoID = -1;
	}
	
	static User[] user;
	static File[] file;
	static Request[] totalrequest;
	
//////////////////////////////////////////////////
	
	static int window, L = 14;	// L=14  3
	static int window_length = 3;
	static int[] hits = new int[L+1];
	static float[] hit_rate = new float [L+1];
	static int[] requests = new int[L+1];
	static Vector<String> request_file = new Vector<String>();	// can duplicate
	
	static int[] cache_size = new int[L+1];
	//static int cache_size = 10000;	// GB
	static Set cache_content = new HashSet();	// can't duplicate
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		user = new User[m_nUserSize];
		
		totalrequest = new Request[m_nRequest];
		
		
		//for(int j=500; j<=15000; j=j+500) {
		for(int j=100; j<=3000; j=j+100) {
		//for(int j=500; j<=3000; j=j+500) {
		cache_content.clear();
		////////
		System.out.println("request\tvideo\thit\tC="+j);
		window = L-1;
		requests[window] = ReadRequestInfo(inputfile, window);	// get request number and request file
		while(window<=L) {
			//System.out.println("window="+window);
			
			// compute hits 
			if(cache_content.isEmpty()) 
				hits[window] = 0;
			else 
				hits[window] = CacheHit();
			
			// update the cache
			
			cache_size[window] = (int)(requests[window]*0.4);	// compute cache size
			
			long startTime = System.currentTimeMillis();
			//UpdateCache(cache_size[window]);
			UpdateCache(j);
			long endTime = System.currentTimeMillis();
			System.out.println(endTime-startTime);  //print time cost of building model 
			
			//System.out.println("request:"+requests[window]+" hit:"+hits[window]+" cache_size:"+cache_size[window]+" cache_content:"+cache_content.size());
			//System.out.println("==========================");
			hit_rate[window] =(float)hits[window]/(float)requests[window]; 
			System.out.println(hit_rate[window]*100);
			
			if(window==L) {
				float sum = 0;
				for(int k=1; k<=L; k++) {
					sum = sum+ hit_rate[k];
				}
				System.out.println(sum/(float)(L-1));
				System.out.println("==========================");
			}
			
			window++;
			if(window<=L)
				requests[window] = ReadRequestInfo(inputfile, window);	// get request number and request file
		}
		////////
		}
			
	}

	private static int ReadRequestInfo(String Input, int window) throws Exception {
		BufferedReader br;
		br = new BufferedReader(new FileReader(Input));
		
		String line;
		
		int client_num = 0;
		int video_num = 0;
		int request_num = 0;
		int Client_ID = -1;
		int Video_ID = -1;
		
		boolean find_the_same = false;
		
		int start_time = -1;
		int end_time = -1;
		int timestamp;
		
		request_file.removeAllElements();
		
		for(int i=0; i<m_nUserSize; i++) {
			user[i] = new User();
		}
		
		file = new File[m_nItemSize];
		for(int i=0; i<m_nItemSize; i++) {
			file[i] = new File();
		}
			
		for(int i=0; i<m_nRequest; i++) {
			totalrequest[i] = new Request();
		}
		
		while((line=br.readLine()) != null) {
			//String items[] = line.split(" ");	// 0:Timestamp 1:YouTubeServerIP 2:ClientIP 3:Request 4:VideoID 5:ContentServerIP 6:CategoryID 7:ViewCount 8:likeCount 9:DislikeCount 10:FavoriteCount 11:CommentCount
			String items[] = line.split(",");	// 0:Timestamp 1:VideoID 2:CategoryID 3:ViewCount 4:likeCount 5:DislikeCount 6:CommentCount 7:LocalView 8:ClientIP
			timestamp = Integer.parseInt((String)items[0].subSequence(3, 10));
			if(start_time==-1) {
				start_time = timestamp + (window-1)*86400;
				end_time = timestamp +window*86400;
			}
			if(end_time<timestamp)
				break;
			
			if(timestamp >= start_time && timestamp <= end_time) {
				request_file.add(items[1]);
				request_num++;  
				
			}
			
			
			for(int i=0; i<user.length; i++) {
				if(user[i].ID==-1)
					break;
				
				if(items[8].equals(user[i].Name)) {
					find_the_same = true;
					Client_ID = user[i].ID;
					break;
				}
			}
			
			if(find_the_same==false) {
				user[client_num].ID = client_num;
				user[client_num].Name = items[8];
				user[client_num].count++;
				Client_ID = user[client_num].ID;
				client_num++;
			}
			find_the_same = false;
			
			for(int i=0; i<file.length; i++) {
				if(file[i].ID==-1) 
					break;
				
	         	if(items[1].equals(file[i].Name)) {
	         		find_the_same = true;
	         		//System.out.println("find the same!");
	         			
	         		Video_ID = file[i].ID;
	         		
	         		if(Integer.valueOf(timestamp)<=timethres) {
	         			file[Video_ID].count_duplicate++;	   
	         		}
	         		
	         		boolean duplicate = false;
	         		for(int request=0; request<totalrequest.length; request++) {
	         			if(totalrequest[request].UserID==Client_ID 
	         					&& totalrequest[request].VideoID==Video_ID
	         					&& Integer.valueOf(timestamp)-totalrequest[request].timestamp < thre_requestfreq) {
	         				duplicate=true;
	         				break;
	         			}
	         		}         		
	         		if(duplicate==false) {
	         			if(Integer.valueOf(timestamp)<=timethres) {
	         				file[Video_ID].count++;
	         			}
	         			user[Client_ID].count++;
	         			file[Video_ID].User.offer(Client_ID);
	             		user[Client_ID].File.offer(Video_ID);
	         		}
	         		
	         		duplicate = false;
	         			
	         		break;
	         	}
			}
				
			if(find_the_same==false) {
	         	file[video_num].ID = video_num;
         		file[video_num].Name = items[1];         		
         		Video_ID = file[video_num].ID;    
         		
         		if(Integer.valueOf(timestamp)<=timethres) {
         			file[Video_ID].count++;
         			file[Video_ID].count_duplicate++;
         		}
         		
     			file[Video_ID].User.offer(Client_ID);
         		user[Client_ID].File.offer(Video_ID);
         		video_num++;
			}
		        
			find_the_same = false;
		        		
	    	totalrequest[request_num].ID = request_num;
			totalrequest[request_num].timestamp = Integer.valueOf(timestamp);	
			totalrequest[request_num].UserID = Client_ID;
			totalrequest[request_num].VideoID = Video_ID;
		}   
			
		//System.out.println("Request num = " + request_num);
		//System.out.println("Client num = " + client_num);
     	//System.out.println("Video num = " + video_num);
		System.out.print(request_num+"\t"+video_num+"\t");
		
		br.close();
     	
		request_file_num = video_num;
		
     	return request_num;
     	//return video_num;
	}

	private static int CacheHit() {
		int hit = 0;
		
		for(String s: request_file)
			if(cache_content.contains(s))
				hit++;
		
		return hit;
	}
	
	private static void UpdateCache(int cache_size) {
		cache_content.clear();
		Arrays.sort(file);
		
		for(int i=0; cache_content.size()<cache_size && i<request_file_num; i++) {
			//System.out.println(i + " " + file[i].Name + " " + file[i].count);
			cache_content.add(file[i].Name);
		}
	}
	
	// no use here, but we can get popular video by sorting file.count
	private static void CalculateNumUser() {
		int requestfile = -1;
		int requestuser = -1;
		
		int fid = 0;
		int fid_30 = 10;	//11~30
		int uid = 0;
		
		for(int r=0; r<totalrequest.length; r++) {
			if(totalrequest[r].timestamp<timethres) {
     			int i = totalrequest[r].UserID;
     			int j = totalrequest[r].VideoID;
     			if(user[i].count>=thres_usercount) {
     				requestfile = j;

     				if(file[requestfile].count>thres_filecount) {//18,5,2	    					
    					if(file[requestfile].Fix_ID==-1) {
    						file[requestfile].Fix_ID = fid;
    						//System.out.println(fid);
    						fid++;
    					}
    					if(user[i].Fix_ID==-1) {
    						user[i].Fix_ID = uid;
    						//System.out.println(uid);
    						uid++;
    					}
    				}
    				else if(file[requestfile].count<thres_filecount && file[requestfile].count>thres_filecount_30) {
    					if(file[requestfile].Fix_ID==-1) {
    						file[requestfile].Fix_ID = fid_30;
    						//System.out.println(fid);
    						fid_30++;
    					}
    				}
    				
     			}
     		}
		}

		System.out.println("user: "+uid+"\t"+"video: "+fid+"\t"+"video_30: "+fid_30);
    	//m_nEndUserSize=uid;
    	//m_nEndItemSize=fid;
    	int rid = 0;
    	for(int i=0; i<m_nRequest; i++) {
    		if(user[totalrequest[i].UserID].Fix_ID!=-1 && file[totalrequest[i].VideoID].Fix_ID!=-1 && totalrequest[i].timestamp<timethres) {
    			rid++;    			
    		}
    	}
    	//m_nEndRequestSize=rid;
    	System.out.println("Request: "+rid);
	}

}



