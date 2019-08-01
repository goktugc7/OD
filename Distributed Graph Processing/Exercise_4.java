package exercise_4;

import com.clearspring.analytics.util.Lists;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.MetadataBuilder;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.graphframes.GraphFrame;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Exercise_4 {
	
	static final double DAMPING_FACTOR = 0.85;
	static final int MAX_ITERATIONS = 100;
	
	public static void wikipedia(JavaSparkContext ctx, SQLContext sqlCtx) {
		
		//Loading data
		List<Row> edgesList = new ArrayList<Row>();
		try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/edges.csv"))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] edges = sCurrentLine.split(",");
				Row rowEdges = RowFactory.create(Long.parseLong(edges[0]), Long.parseLong(edges[1]));
				edgesList.add(rowEdges);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		StructType edges_schema = new StructType(new StructField[]{
				new StructField("src", DataTypes.LongType, false, new MetadataBuilder().build()),
				new StructField("dst", DataTypes.LongType, false, new MetadataBuilder().build())
			});		
		List<Row> verticesList = new ArrayList<Row>();
		try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/vertices.csv"))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] edges = sCurrentLine.split(",");
				Row rowVertex = RowFactory.create(Long.parseLong(edges[0]), edges[1]);
				verticesList.add(rowVertex);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		StructType vertices_schema = new StructType(new StructField[]{
				new StructField("id", DataTypes.LongType, false, new MetadataBuilder().build()),
				new StructField("title", DataTypes.StringType, false, new MetadataBuilder().build())
			});
		
		
		JavaRDD<Row> vertices_rdd = ctx.parallelize(verticesList);
		Dataset<Row> vertices =  sqlCtx.createDataFrame(vertices_rdd, vertices_schema);
		JavaRDD<Row> edges_rdd = ctx.parallelize(edgesList);
		Dataset<Row> edges = sqlCtx.createDataFrame(edges_rdd, edges_schema);	
		
		GraphFrame gf = GraphFrame.apply(vertices,edges);
		
		gf.pageRank().maxIter(MAX_ITERATIONS).resetProbability(1-DAMPING_FACTOR).run().vertices().select("id", "title", "pagerank").orderBy(org.apache.spark.sql.functions.col("pagerank").desc()).show(10);;

	}
}
