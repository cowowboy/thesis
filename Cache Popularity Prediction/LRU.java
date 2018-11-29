package performance.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Vector;

public class LRU {
	static String inputfile = "test_20180920";	// training_A.txt  training.txt
	
	static int L = 7;	// L=14  3
	static String line;
	static Vector<String> request_file = new Vector<String>();	// can duplicate
	static Vector<String> All_request_file = new Vector<String>();
	
	static class Node{
	    String key;
	    int value;
	    Node pre;
	    Node next;
	 
	    public Node(String key, int value){
	        this.key = key;
	        this.value = value;
	    }
	}
	
	public static class LRUCache {
	    int capacity;
	    HashMap<String, Node> map = new HashMap<String, Node>();
	    Node head=null;
	    Node end=null;
	 
	    public LRUCache(int capacity) {
	        this.capacity = capacity;
	    }
	 
	    public int get(String key) {
	        /*if(map.containsKey(key)){
	            Node n = map.get(key);
	            remove(n);
	            setHead(n);
	            return n.value;
	        }
	 
	        return -1;*/
	    	if(map.containsKey(key))
	    		return 1;
	    	else
	    		return -1;
	    }
	 
	    public void remove(Node n){
	        if(n.pre!=null){
	            n.pre.next = n.next;
	        }else{
	            head = n.next;
	        }
	 
	        if(n.next!=null){
	            n.next.pre = n.pre;
	        }else{
	            end = n.pre;
	        }
	 
	    }
	 
	    public void setHead(Node n){
	        n.next = head;
	        n.pre = null;
	 
	        if(head!=null)
	            head.pre = n;
	 
	        head = n;
	 
	        if(end ==null)
	            end = head;
	    }
	 
	    public void set(String key, int value) {
	        if(map.containsKey(key)){
	            Node old = map.get(key);
	            old.value = value;
	            remove(old);
	            setHead(old);
	        }else{
	            Node created = new Node(key, value);
	            if(map.size()>=capacity){
	                map.remove(end.key);
	                remove(end);
	                setHead(created);
	 
	            }else{
	                setHead(created);
	            }    
	            
	           map.put(key, created);
	        }
	    }
	    
	    public boolean empty() {
	    	return map.isEmpty();
	    }
	    
	    public void clear() {
	    	map.clear();
	    	
	    	head=null;
	    	end=null;
	    }

	}
	
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		int hits;
		float hit_rate [] = new float [L+1]; 
		
		
		//for(int i=100; i<=100; i=i+500) {
		for(int i=5000; i<=50000; i=i+5000) { 
			LRUCache cache = new LRUCache(i);

			System.out.println("request\thit\tC="+i);
			for(int window=1; window<=L; window++) {
					
				request_file.removeAllElements();
				
				int start_time = -1;
				int end_time = -1;
				int request_num = 0;
				int timestamp;
				
				BufferedReader br;
				br = new BufferedReader(new FileReader(inputfile));
				while((line=br.readLine()) != null) {
					String items[] = line.split(",");	// 0:Timestamp 1:VideoID 2:CategoryID 3:ViewCount 4:likeCount 5:DislikeCount 6:CommentCount 7:LocalView
					//1201639761.780669
					//1201639761.78066
					timestamp = Integer.parseInt((String)items[0].subSequence(3, 10));
					if(start_time==-1) {
						start_time = timestamp + (window-1)*86400;
						end_time = timestamp +window*86400;
						System.out.print(start_time-end_time);  //print interval of time where we take video
					}
					if(end_time<timestamp) 
						break;
					
					if(timestamp >= start_time && timestamp <= end_time) {
						
						request_file.add(items[1]);
						All_request_file.add(items[1]);
						request_num++;  
						
					}
				}
				
				br.close();
				
				// Cache Hit
				hits = 0;
				if(!cache.empty()) {
					//System.out.println("hello");
					for(String s: request_file)
						if(cache.get(s)!=-1)
							hits++;
					//cache.clear();
				}
				
				//build cache
				long startTime = System.currentTimeMillis();
				int index = 0;
				for(String s: All_request_file)
					cache.set(s, index++);
				
				long endTime = System.currentTimeMillis();
				
				System.out.println(endTime-startTime);
				
				hit_rate[window] = (float)hits/(float)request_num;
				System.out.println(request_num+"\t"+hits+"\t"+hit_rate[window]*100);
				
				if(window==L) {
					float sum = 0;
					for(int k=1; k<=L; k++) {
						sum = sum+ hit_rate[k];
					}
					System.out.println(sum/(float)(L-1));
					System.out.println("==========================");
				}
			}
		}
	}
	
	
	
}
