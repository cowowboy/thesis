package performance.evaluation;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.clusterers.Canopy;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.RandomizableClusterer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class MyResearch {
	static String inputfile = "test_20180920";	// training_A.txt  training.txt
	static String filename = "training.arff";
	
	static Instances data, newData;
	
	static Canopy canopy;
	static SimpleKMeans simpleKmeans;
	static ClusterEvaluation eval_1;
	static ClusterEvaluation eval_2;
	
	static int cluster_number = 0;
	static int target_cluster[];
	static int target_cluster_rank[];
	
	static int request_file_num;
	static int big = 0;
//////////////////////////////////////////////////	
	
	public static class Attribute implements Comparable<Attribute>{
		String video_ID;
		int video_view;
		String view_count;
		String like_count;
		String dislike_count;
		String comment_count;
		String local_view;
		int cluster = 0;
		int cluster_video_view = 0;
		//int cluster_video_count;
		
		public int compareTo(Attribute arg) {
			if(this.cluster_video_view-arg.cluster_video_view!=0)
				return arg.cluster_video_view-this.cluster_video_view;
			else if(this.video_view-arg.video_view!=0)
				return arg.video_view-this.video_view;
			else
				return 0;
		}
	}
	
	// 30332?
	static int m_nItemSize = 303332 ;	// 137016  303332
	static Attribute[] attribute;
	
//////////////////////////////////////////////////
	
	static int window, L = 7;	// L=3
	static int window_length = 3;
	static int[] hits = new int[L+1];
	static float[] hit_rate = new float[L+1];
	static int[] requests = new int[L+1];
	static Vector<String> request_file = new Vector<String>();	// can duplicate
	
	static int[] cache_size = new int[L+1];
	//static int cache_size = 10000;	// GB
	static Set cache_content = new HashSet();	// can't duplicate
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		
		
		/*
		window = 10;
		requests[window] = ReadRequestInfo(inputfile, window);
		BuildModel();	// generate newData
		UpdateCache(30000);
		*/
		
		//for(int j=500; j<=3000; j=j+500) {
		for(int j=5000; j<=50000; j=j+5000) {
		cache_content.clear();
		////////
		System.out.println("request\tvideo\tK\thit\tC="+j);
		window = L-1; //window = 1
		requests[window] = ReadRequestInfo(inputfile, window);
		while(window<=L) {
			//System.out.println("window="+window);
			
			// compute hits 
			if(cache_content.isEmpty())
				hits[window] = 0;
			else
				hits[window] = CacheHit();
			
			// update the cache
			
			long startTime = System.currentTimeMillis();
			
			BuildModel();	// generate newData
			cache_size[window] = (int)(requests[window]*0.4);	// compute cache size
			
			long endTime = System.currentTimeMillis();
			
			//System.out.println(endTime-startTime);  //print time cost of building model 
			
			//UpdateCache(cache_size[window]);
			UpdateCache(j);
			
			//System.out.println("request:"+requests[window]+" hit:"+hits[window]+" cache_size:"+cache_size[window]+" cache_content:"+cache_content.size());
			//System.out.println(requests[window]+"\t"+hits[window]+"\t"+cache_size[window]+"\t"+cache_content.size());
			//System.out.println(hits[window]);
			hit_rate[window] = (float)hits[window]/(float)requests[window];
			System.out.println(hit_rate[window]*100);
			
			if(window==L) {
				float sum = 0;
				for(int i=1; i<=L; i++) {
					sum = sum+ hit_rate[i];
				}
				System.out.println(sum/(float)(L-1));
				System.out.println("==========================");
			}
			
			
			
			window++;
			if(window<=L)
				requests[window] = ReadRequestInfo(inputfile, window);
		}
		////////
		}
				
		
	}
	
	private static int ReadRequestInfo(String Input, int window) throws Exception {
		BufferedReader br;
		br = new BufferedReader(new FileReader(Input));
		
		attribute = new Attribute[m_nItemSize];
		
		for(int i=0; i<m_nItemSize; i++)
			attribute[i] = new Attribute();
		
		FileWriter fw = new FileWriter(filename);  
		BufferedWriter bw = new BufferedWriter(fw); 
		
		int vView[] = new int [m_nItemSize];
		String vCategory[] = new String[m_nItemSize];
		String vStatistics[] = new String[m_nItemSize];
		Map<String, Integer> video = new HashMap<String, Integer>();
		
		int Vindex = 0;
		int request_num = 0;
		String line;
		
		int start_time = -1;
		int end_time = -1;
		int timestamp;
		
		
		request_file.removeAllElements();
		
		while((line=br.readLine()) != null) {
			//String items[] = line.split(" ");	// 0:Timestamp 1:YouTubeServerIP 2:ClientIP 3:Request 4:VideoID 5:ContentServerIP 6:CategoryID 7:ViewCount 8:likeCount 9:DislikeCount 10:FavoriteCount 11:CommentCount
			String items[] = line.split(",");	// 0:Timestamp 1:VideoID 2:CategoryID 3:ViewCount 4:likeCount 5:DislikeCount 6:CommentCount 7:LocalView
			
			timestamp = Integer.parseInt((String)items[0].subSequence(3, 10));
			if(start_time==-1) {
				start_time = timestamp + (window-1)*86400;
				end_time = timestamp +window*86400;
				//System.out.print("\t"+start_time + "\t" + end_time + "\t");
			}
			if(end_time<timestamp) 
				break;
			
			if(timestamp >= start_time && timestamp <= end_time) {
				
				request_file.add(items[1]);
				request_num++;  
				
			}
			
			if(video.containsKey(items[1])==false) {
				video.put(items[1], Vindex);
				Vindex++;
			}
			int i = video.get(items[1]);
			
			vView[i]++;
			
			if(vCategory[i]==null)
				vCategory[i] = items[2];
			
			if(vStatistics[i]==null) {
				vStatistics[i] = items[3]+","+items[4]+","+items[5]+","+items[6]+","+items[7];	// #viewCount likeCount dislikeCount commentCount
				vStatistics[i] = vStatistics[i].replaceAll("null", "0");
			}
			
		}
		
		bw.write("@RELATION video_view\n\n");
		
		bw.write("@ATTRIBUTE Video_ID STRING\n");	//bw.write("@ATTRIBUTE Video_ID NUMERIC\n");
		bw.write("@ATTRIBUTE Video_View NUMERIC\n");
		//bw.write("@ATTRIBUTE Video_Client NUMERIC\n");
		bw.write("@ATTRIBUTE Video_Category {1,2,10,15,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44}\n");	//bw.write("@ATTRIBUTE Video_Category NUMERIC\n");
		bw.write("@ATTRIBUTE View_Count NUMERIC\n");
		bw.write("@ATTRIBUTE Like_Count NUMERIC\n");
		bw.write("@ATTRIBUTE Dislike_Count NUMERIC\n");
		bw.write("@ATTRIBUTE Comment_Count NUMERIC\n");
		//bw.write("@ATTRIBUTE Favorite_Count NUMERIC\n");	// always be 0
		bw.write("@ATTRIBUTE Local_View NUMERIC\n\n");
		
		bw.write("@DATA\n");
		
		Set<String> key = video.keySet();

		for(String s: key) {
			int i = video.get(s);
			bw.write(s + "," + vView[i] + "," + vCategory[i] + "," + vStatistics[i] +"\n");
		}

		bw.close();
		br.close();
		
		System.out.print(request_num+"\t"+video.size()+"\t");
		
		request_file_num = video.size();
		
		return request_num;
		//return video.size();
	}
	
	private static void BuildModel() throws Exception {
		// load data
		data  = new Instances(new BufferedReader(new FileReader(filename)));
		//if(data.classIndex()==-1)
			//data.setClassIndex(data.numAttributes()-1);	// last attribute is defined for classification
		//System.out.println("load data...");
		
		// generate data for clusterer (w/o class)
	    String[] options = new String[2];	// attribute: Video_Category
	    options[0] = "-R";					// range
	    options[1] = "1,3";					// first and third attribute (videoID,categoryID)
	    Remove remove = new Remove();		// new instance of filter
	    remove.setOptions(options);			// set options
	    remove.setInputFormat(data);		// inform filter about dataset **AFTER** setting options
	    newData = Filter.useFilter(data,remove);
	    //System.out.println("generate data...");

//////////////////////////////////////////////////////////////
	    
	    // first train clusterer
	    canopy = new Canopy();
	    // set further options for Canopy, if necessary...
	    canopy.buildClusterer(newData);
	    //System.out.println("train clusterer...");
	    
	    // first evaluate clusterer
	    eval_1 = new ClusterEvaluation();
	    eval_1.setClusterer(canopy);
	    eval_1.evaluateClusterer(newData);
	    //System.out.println("evaluate clusterer...");

	    // print results
	    //System.out.println(eval.clusterResultsToString());
	    //System.out.println("# of clusters: " + eval.getNumClusters()); 
	    cluster_number = eval_1.getNumClusters();
	    //cluster_number = 8;
	    System.out.print(cluster_number+"\t");
	    
//////////////////////////////////////////////////////////////
	    
	    // second train clusterer
	    simpleKmeans = new SimpleKMeans();
	    simpleKmeans.setNumClusters(cluster_number);  
	    // set further options for Canopy, if necessary...
	    simpleKmeans.buildClusterer(newData);
	    //System.out.println("train clusterer...");
	    
	    // second evaluate clusterer
	    eval_2 = new ClusterEvaluation();
	    eval_2.setClusterer(simpleKmeans);
	    eval_2.evaluateClusterer(newData);
	    //System.out.println("evaluate clusterer...");
	    
	    // print results
	    //System.out.println(eval_2.clusterResultsToString());
	     
//////////////////////////////////////////////////////////////
	    
	    // print cluster results
	    double[] temp = eval_2.getClusterAssignments();
	    /*for(int i=0; i<newData.numInstances(); i++) {	
	    	System.out.print(data.get(i));	// s5_xT3HsaLU,1,22,166,1,0,1
	    	System.out.println(",cluster"+(int)temp[i]);	   
	    }*/
	    
//////////////////////////////////////////////////////////////    
	    // cluster result
	    target_cluster = new int[cluster_number];
	    target_cluster_rank = new int[cluster_number];
	    int start = eval_2.clusterResultsToString().lastIndexOf("View_Count");	// View_Count	// Video_View
	    int end = eval_2.clusterResultsToString().lastIndexOf("Like_Count");	// Like_Count	// View_Count
	    String items[] = eval_2.clusterResultsToString().substring(start,end).split("\\s+");
	    int mx = 0;
	    for(int i=0; i<eval_2.getNumClusters(); i++) {
	    	target_cluster[i] = (int)(Double.parseDouble(items[i+2]));
	    	/*if(target_cluster[i]>mx) {
	    		mx = target_cluster[i];
	    		big = i;
	    	}*/
	    }
	    
	    System.arraycopy(target_cluster, 0, target_cluster_rank, 0, target_cluster.length);
	    Arrays.sort(target_cluster_rank);
	    
	    
	    for(int i=0; i<cluster_number; i++) {
	    	for(int j=0; j<cluster_number; j++) {
		    	if(target_cluster[i]==target_cluster_rank[j])
		    		target_cluster_rank[j]=i;
		    }
	    }
	    
	    /*for(int i=0; i<eval_2.getNumClusters(); i++)
	    	System.out.print(target_cluster_rank[i]+"\t");
	    for(int i=0; i<eval_2.getNumClusters()+2; i++) {
	    	System.out.print(items[i]+"\t");
	    }*/
	   
	       
	    double[] assign = eval_2.getClusterAssignments();
	    
	    for(int i=0; i<data.numInstances(); i++) {
	    	String item[] = data.get(i).toStringNoWeight().split(",");	// 0:video_id 1:video_view 2:video_category 3:view_count 4:like_count 5:dislike_count 6:comment_count
	    	attribute[i].video_ID = item[0];
	    	attribute[i].video_view = Integer.parseInt(item[1]);
	    	attribute[i].view_count = item[3];
	    	attribute[i].like_count = item[4];
	    	attribute[i].dislike_count = item[5];
	    	attribute[i].comment_count = item[6];
	    	attribute[i].local_view = item[7];
	    	attribute[i].cluster = (int)assign[i];
	    	attribute[i].cluster_video_view = target_cluster[attribute[i].cluster];
	    }
	}
	
	private static int CacheHit() {
		int hit = 0;
		
		for(String s: request_file) {	
			if(cache_content.contains(s)) {
				hit++;
				continue;
			}
				
		}
		return hit;
	}
	
	private static void UpdateCache(int cache_size) {
		cache_content.clear();
		Arrays.sort(attribute);
		int rank = target_cluster_rank.length-1;
		while(cache_content.size()<cache_size && rank>=0) {
			for(int i=0; cache_content.size()<cache_size && i<request_file_num; i++) {
				if(attribute[i].cluster==target_cluster_rank[rank])
					cache_content.add(attribute[i].video_ID);
					
		    }
			rank--;
		}
		/*for(int i=0; cache_content.size()<cache_size && i<request_file_num; i++) {
			if(attribute[i].cluster!=big)
				cache_content.add(attribute[i].video_ID);
		}*/
	}
}
