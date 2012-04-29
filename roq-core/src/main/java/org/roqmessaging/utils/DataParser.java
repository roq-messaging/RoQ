package org.roqmessaging.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class DataParser {
	
	
	public static void main(String[] args) throws Exception{
		System.out.println("Starting data parser");
		File basedir = new File(args[0]+"/output.csv");
		processExchg(basedir);

	}
	
	public static void processExchg(File file) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		BufferedWriter exchgWriter = new BufferedWriter(new FileWriter(file.getParent()+"/exchg.csv"));
		BufferedWriter listWriter = new BufferedWriter(new FileWriter(file.getParent()+"/list.csv"));
		BufferedWriter prodWriter = new BufferedWriter(new FileWriter(file.getParent()+"/prod.csv"));
		
		BufferedWriter writer = null;
		
		for (String line = reader.readLine();line != null && !line.isEmpty();line = reader.readLine()) {
			String[] data = line.split(",");
			
			if (data[0].equals("EXCH")){
				writer = exchgWriter;
			}
			
			else if (data[0].equals("LIST")){
				writer = listWriter;
			}				
			
			else if (data[0].equals("PROD")){
				writer = prodWriter;
			}

			else{
				System.out.println("Error on line " + data[0]);
				System.exit(1);
			}

			String tempExchg = new String(data[1].substring(11,13)+","+data[1].substring(14,16)+","+data[1].substring(17));
			for (int i = 2 ; i < data.length ; i++) {
				tempExchg += ","+data[i];
			}
			System.out.println(tempExchg);
			writer.write(tempExchg);
			writer.newLine();

		}

		exchgWriter.close();
		listWriter.close();
		
	}

}
