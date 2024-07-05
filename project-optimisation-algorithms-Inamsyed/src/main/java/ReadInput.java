package main.java;

import java.io.*;
import java.util.*;

public class ReadInput {
    public Map<String, Object> data;

    // Constructor
    public ReadInput() {
        data = new HashMap<String, Object>(); // Initilising the Map
    }

    public double fitness(int[][] array) {

        int numOfCache = array.length;
        int numOfVideo = array[0].length;
        int cacheSize = (int) data.get("cache_size");
        int[] videoSizeArray = (int[]) data.get("video_size_desc");
        ArrayList<Integer> EP_to_DC_LatencyList = (ArrayList<Integer>) data.get("ep_to_dc_latency");
        ArrayList<ArrayList<Integer>> EP_to_Cache_Latency_LofL = (ArrayList<ArrayList<Integer>>) data
                .get("ep_to_cache_latency");

        // Feasibility Check
        for (int i = 0; i < numOfCache; i++) {
            int total = 0;
            for (int j = 0; j < numOfVideo; j++) {
                if (array[i][j] == 1) {
                    total += videoSizeArray[j];
                }
            }
            if (total > cacheSize) {
                return -1;
            }
        }

        HashMap<String, String> map = (HashMap<String, String>) data.get("video_ed_request");
        double sumOfAllReq = 0;
        double sumOfAllGains = 0;

        for (String key : map.keySet()) {
            String[] videoAndEndPointID = key.split(",");
            int videoID = Integer.parseInt(videoAndEndPointID[0]);
            int endPointID = Integer.parseInt(videoAndEndPointID[1]);
            int numOfRequests = Integer.parseInt(map.get(key));
            int videoFileSize = videoSizeArray[videoID];
            int endPointLatencyDC = EP_to_DC_LatencyList.get(endPointID);
            sumOfAllReq += numOfRequests;

            int lowestCacheCost = endPointLatencyDC;
            ArrayList<Integer> EndPoint_Cache_LatencyList = EP_to_Cache_Latency_LofL.get(endPointID);
            for (int i = 0; i < EndPoint_Cache_LatencyList.size(); i++) {
                if (EndPoint_Cache_LatencyList.get(i) < endPointLatencyDC && array[i][videoID] == 1) {
                    lowestCacheCost = Math.min(lowestCacheCost, EndPoint_Cache_LatencyList.get(i));
                }
            }
            int diffLowCacheDataC = endPointLatencyDC - lowestCacheCost;
            sumOfAllGains += (diffLowCacheDataC * numOfRequests);
        }

        double avgTimeSaved = (sumOfAllGains / sumOfAllReq) * 1000;

        return avgTimeSaved;
    }

    public int[][] HillClimbing() {
        int numOfCache = (int) data.get("number_of_caches");
        int numOfVideos = (int) data.get("number_of_videos");
        int[][] array = new int[numOfCache][numOfVideos];
        double bestScore = 0;
        int bestIMove = 0;
        int bestJMove = 0;
        boolean improvement = true;

        while (improvement == true) {
            improvement = false;
            for (int i = 0; i < array.length; i++) {
                for (int j = 0; j < array[0].length; j++) {
                    if (array[i][j] != 1) {
                        array[i][j] = 1;
                        if (fitness(array) > bestScore) {
                            bestScore = fitness(array);
                            bestIMove = i;
                            bestJMove = j;
                            improvement = true;
                        }
                        array[i][j] = 0;
                    }
                }
            }
            array[bestIMove][bestJMove] = 1;
        }

        return array;
    }

    public void GeneticAlgorithm() {
        int numOfRows = (int) data.get("number_of_caches");
        int numOfColumns = (int) data.get("number_of_videos");
        double Crossover_Bias = 0.2; // Probability of a crossover
        double Mutation_Bias = 1 / (numOfRows * numOfColumns); // Probability of a Mutation
        Random random = new Random();
        ArrayList<int[][]> population = new ArrayList<>();

        // Initilising a Population of Solutions
        for (int i = 0; i < 50; i++) {
            population.add(randomSolution(numOfRows, numOfColumns));
        }

        for (int generation = 0; generation < 100; generation++) {

            int populationSize = population.size(); // Variable to store size as it gets corrupted each time
            System.out.println("Start Population Size : " + populationSize);

            // Crossover
            for (int i = 0; i < populationSize - 1; i++) {
                double x = random.nextDouble();
                if (x <= Crossover_Bias) {
                    int[][] child1 = CrossOver(population.get(i), population.get(i + 1));
                    int[][] child2 = CrossOver(population.get(i + 1), population.get(i));
                    if (fitness(child1) != -1) {
                        population.add(child1);
                    }
                    if (fitness(child2) != -1) {
                        population.add(child2);
                    }
                }
            }

            // Mutation
            for (int i = 0; i < population.size(); i++) {
                double x = random.nextDouble();
                if (x <= Mutation_Bias) {
                    Mutate(population.get(i));
                }
            }

            // Pick the best 50
            population = Survival(population);
        }

    }

    // Creates an Array and populates it randomly with 0s and 1s
    public int[][] randomSolution(int numOfRows, int numOfColumns) {
        int[][] array = new int[numOfRows][numOfColumns];
        Random random = new Random();
        double Num_Of_0s_Bias = 0.99; // Controls if I want more 0s in random 2d array

        for (int i = 0; i < numOfRows; i++) {
            for (int j = 0; j < numOfColumns; j++) {
                double x = random.nextDouble();
                array[i][j] = (x < Num_Of_0s_Bias) ? 0 : 1;
            }
        }
        return array;
    }

    // Creates a child Array from two Parent Arrays
    public int[][] CrossOver(int[][] array1, int[][] array2) {
        int[][] result = new int[array1.length][array1[0].length];

        for (int i = 0; i < array1.length; i++) {
            result[i] = (i < array1.length / 2) ? array1[i] : array2[i];
        }
        return result;
    }

    // Causes Mutation within an Element
    public void Mutate(int[][] array) {
        Random random = new Random();
        int row = random.nextInt(array.length);
        int column = random.nextInt(array[0].length);
        array[row][column] = (array[row][column] == 0) ? 1 : 0;
        return;
    }

    // Chooses the 50 Fittest indviduals/Elements
    public ArrayList<int[][]> Survival(ArrayList<int[][]> population) {
        HashMap<Integer, Double> map = new HashMap<>();
        ArrayList<int[][]> result = new ArrayList<>();
        int index = 0;
        int numOfElem = 50; // How many elements you want to pick from population

        // Populate the HashMap
        for (int[][] element : population) {
            map.put(index, fitness(element));
            index++;
        }

        PriorityQueue<Map.Entry<Integer, Double>> pq = new PriorityQueue<>(
                Comparator.comparingDouble((Map.Entry<Integer, Double> entry) -> entry.getValue()).reversed());

        // Add entries from the HashMap to the PriorityQueue
        for (Map.Entry<Integer, Double> entry : map.entrySet()) {
            pq.offer(entry); // Use offer() instead of add() for PriorityQueue
        }

        System.out.println("Highest Fitness in this Generation" + pq.peek());

        // Populate the result array list with the 50 fittest 2d Arrays
        for (int i = 0; i < numOfElem; i++) {
            result.add(population.get(pq.poll().getKey()));
        }

        return result;
    }

    // Counts number of fit indviduals within population
    public int fitCount(ArrayList<int[][]> population) {
        int count = 0;
        for (int i = 0; i < population.size(); i++) {
            if (fitness(population.get(i)) != -1) {
                count++;
            }
        }
        return count;
    }

    // Prints the 2d Array
    public void printArray(int[][] array) {
        for (int[] x : array) {
            System.out.println(Arrays.toString(x));
        }
        System.out.println();
    }

    // Function belonging to the Class ReadInput
    // Filename variable holds the path to the input file
    public void readGoogle(String filename) throws IOException {

        // FileReader opens the file for reading. It reads char from file
        // BufferedReader provides efficient reading of characters
        BufferedReader fin = new BufferedReader(new FileReader(filename));

        // This block reads the first line of input file
        // Stores it as a String
        // Then breaks the String using space as a delimeter
        // The broken String is now an array of Strings stored in an array
        // Each element of the array represents system description data
        // You convert each element of the String array into an Int
        // At the end of this function we see these variables/values on HashMap
        String system_desc = fin.readLine();
        String[] system_desc_arr = system_desc.split(" ");
        int number_of_videos = Integer.parseInt(system_desc_arr[0]);
        int number_of_endpoints = Integer.parseInt(system_desc_arr[1]);
        int number_of_requests = Integer.parseInt(system_desc_arr[2]);
        int number_of_caches = Integer.parseInt(system_desc_arr[3]);
        int cache_size = Integer.parseInt(system_desc_arr[4]);

        // This block will read in next line of the input file (Video size Description)
        // Again you first read in the whole line as one string
        // You split the string into String array containing data as Strings
        // You create an Int array of the same length
        // We do this because we want to copy from String data --> Int data
        // At the end you are left with an Int array containing Video Size Description
        // video_size_desc is in our main HashMap (data)

        // video_size_desc array => index = video Number , Value = size of video
        Map<String, String> video_ed_request = new HashMap<String, String>();
        String video_size_desc_str = fin.readLine();
        String[] video_size_desc_arr = video_size_desc_str.split(" ");
        int[] video_size_desc = new int[video_size_desc_arr.length];
        for (int i = 0; i < video_size_desc_arr.length; i++) {
            video_size_desc[i] = Integer.parseInt(video_size_desc_arr[i]);
        }

        // All three of these lists are in our main HashMap
        // They initialize endpoint-to-cache mapping and latency information
        // ep_to_dc_latency = end point to data Center Latency
        // ep_to_cache_latency = end point to Cache server Latency
        List<List<Integer>> ed_cache_list = new ArrayList<List<Integer>>();
        List<Integer> ep_to_dc_latency = new ArrayList<Integer>();
        List<List<Integer>> ep_to_cache_latency = new ArrayList<List<Integer>>();

        // i = current endpoint
        // we first add a default value of 0 for ep_to_dc_latency list
        // ep_to_dc_latency list :
        // index => endpoint value => latency from Data center to that endpoint
        // ep_to_cache_latency = List of list
        // each endpoint could be connected to multiple caches hence a list for each
        // endpoint
        // We take in the description of the endpoint endpoint_desc_arr
        // endpoint_desc_arr include DC latency and num of caches
        // We set the DC latency for that particular endpoint

        // We initialize the CACHE latencies for that particular EP using a loop
        // We initialize all of the CACHE latencies to DC latency + 1
        // ep_to_cache_latency.get(i) => go to the list of the endpoint i
        // .add(ep_to_dc_latency.get(i) + 1) => add (DC Latency + 1) to the list

        // ed_cache_list = a list of lists showing what caches are connected to each
        // endpoint

        for (int i = 0; i < number_of_endpoints; i++) {
            ep_to_dc_latency.add(0);
            ep_to_cache_latency.add(new ArrayList<Integer>());

            String[] endpoint_desc_arr = fin.readLine().split(" ");
            int dc_latency = Integer.parseInt(endpoint_desc_arr[0]);
            int number_of_cache_i = Integer.parseInt(endpoint_desc_arr[1]);
            ep_to_dc_latency.set(i, dc_latency);

            for (int j = 0; j < number_of_caches; j++) {
                ep_to_cache_latency.get(i).add(ep_to_dc_latency.get(i) + 1);
            }

            List<Integer> cache_list = new ArrayList<Integer>();
            for (int j = 0; j < number_of_cache_i; j++) {
                String[] cache_desc_arr = fin.readLine().split(" ");
                int cache_id = Integer.parseInt(cache_desc_arr[0]);
                int latency = Integer.parseInt(cache_desc_arr[1]);
                cache_list.add(cache_id);
                ep_to_cache_latency.get(i).set(cache_id, latency);
            }
            ed_cache_list.add(cache_list);
        }

        for (int i = 0; i < number_of_requests; i++) {
            String[] request_desc_arr = fin.readLine().split(" ");
            String video_id = request_desc_arr[0];
            String ed_id = request_desc_arr[1];
            String requests = request_desc_arr[2];
            video_ed_request.put(video_id + "," + ed_id, requests);
        }

        data.put("number_of_videos", number_of_videos);
        data.put("number_of_endpoints", number_of_endpoints);
        data.put("number_of_requests", number_of_requests);
        data.put("number_of_caches", number_of_caches);
        data.put("cache_size", cache_size);
        data.put("video_size_desc", video_size_desc);
        data.put("ep_to_dc_latency", ep_to_dc_latency);
        data.put("ep_to_cache_latency", ep_to_cache_latency);
        data.put("ed_cache_list", ed_cache_list);
        data.put("video_ed_request", video_ed_request);

        fin.close();

    }

    public String toString() {
        String result = "";

        // for each endpoint:
        for (int i = 0; i < (Integer) data.get("number_of_endpoints"); i++) {
            result += "enpoint number " + i + "\n";
            // latendcy to DC
            int latency_dc = ((List<Integer>) data.get("ep_to_dc_latency")).get(i);
            result += "latency to dc " + latency_dc + "\n";
            // for each cache
            for (int j = 0; j < ((List<List<Integer>>) data.get("ep_to_cache_latency")).get(i).size(); j++) {
                int latency_c = ((List<List<Integer>>) data.get("ep_to_cache_latency")).get(i).get(j);
                result += "latency to cache number " + j + " = " + latency_c + "\n";
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        ReadInput ri = new ReadInput();
        ri.readGoogle("input/example.in");
        System.out.println(ri.data.get("video_ed_request"));
        System.out.println(ri.toString());

        ri.HillClimbing();
        ri.GeneticAlgorithm();
    }
}

// Latency to cache is always less vs latency to DC