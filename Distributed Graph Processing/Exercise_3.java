package exercise_3;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.graphx.*;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConverters;
import scala.reflect.ClassTag$;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;
import scala.runtime.AbstractFunction3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Exercise_3 {
    
    private static class VProg extends AbstractFunction3<Long,ShortPath,ShortPath,ShortPath> implements Serializable {

        @Override
        public ShortPath apply(Long vertexID, ShortPath vertexValue, ShortPath message) {
            Integer value = message.getCost();
            if (value == Integer.MAX_VALUE) {             // superstep 0
                return vertexValue;
            } else {                                        // superstep > 0
                message.addToPath(vertexID);
                return new ShortPath(Math.min(vertexValue.getCost(), value), message.getPath());
            }
        }
    }

    private static class sendMsg extends AbstractFunction1<EdgeTriplet<ShortPath,Integer>, Iterator<Tuple2<Object,ShortPath>>> implements Serializable {
        @Override
        public Iterator<Tuple2<Object, ShortPath>> apply(EdgeTriplet<ShortPath, Integer> triplet) {
            Tuple2<Object,ShortPath> originVertex = triplet.toTuple()._1();
            Tuple2<Object,ShortPath> destVertex = triplet.toTuple()._2();

            if (originVertex._2.getCost() > destVertex._2.getCost() || originVertex._2.getCost() == Integer.MAX_VALUE) {
                return JavaConverters.asScalaIteratorConverter(new ArrayList<Tuple2<Object,ShortPath>>().iterator()).asScala();
            } else {
            	originVertex._2.setCost(originVertex._2.getCost() + triplet.toTuple()._3());
                return JavaConverters.asScalaIteratorConverter(Arrays.asList(new Tuple2<Object,ShortPath>(triplet.dstId(), originVertex._2)).iterator()).asScala();
            }
        }
    }

    private static class merge extends AbstractFunction2<ShortPath,ShortPath,ShortPath> implements Serializable {
        @Override
        public ShortPath apply(ShortPath o, ShortPath o2) {
            if (o.getCost() <= o2.getCost()) {
            	return o2;
            }
            else {
            	return o;
            }
        }
    }
    
    public static void shortestPathsExt(JavaSparkContext ctx) {
        Map<Long, String> labels = ImmutableMap.<Long, String>builder()
                .put(1l, "A")
                .put(2l, "B")
                .put(3l, "C")
                .put(4l, "D")
                .put(5l, "E")
                .put(6l, "F")
                .build();

        List<Tuple2<Object,ShortPath>> vertices = Lists.newArrayList(
                new Tuple2<>(1L,new ShortPath(0, 1L)),
                new Tuple2<>(2L,new ShortPath(Integer.MAX_VALUE, 2L)),
                new Tuple2<>(3L,new ShortPath(Integer.MAX_VALUE, 3L)),
                new Tuple2<>(4L,new ShortPath(Integer.MAX_VALUE, 4L)),
                new Tuple2<>(5L,new ShortPath(Integer.MAX_VALUE, 5L)),
                new Tuple2<>(6L,new ShortPath(Integer.MAX_VALUE, 6L))
        );
        List<Edge<Integer>> edges = Lists.newArrayList(
                new Edge<>(1L,2L, 4), // A --> B (4)
                new Edge<>(1L,3L, 2), // A --> C (2)
                new Edge<>(2L,3L, 5), // B --> C (5)
                new Edge<>(2L,4L, 10), // B --> D (10)
                new Edge<>(3L,5L, 3), // C --> E (3)
                new Edge<>(5L, 4L, 4), // E --> D (4)
                new Edge<>(4L, 6L, 11) // D --> F (11)
        );

        JavaRDD<Tuple2<Object,ShortPath>> verticesRDD = ctx.parallelize(vertices);
        JavaRDD<Edge<Integer>> edgesRDD = ctx.parallelize(edges);

        Graph<ShortPath,Integer> G = Graph.apply(verticesRDD.rdd(),edgesRDD.rdd(),new ShortPath(Integer.MAX_VALUE), StorageLevel.MEMORY_ONLY(), StorageLevel.MEMORY_ONLY(),
                scala.reflect.ClassTag$.MODULE$.apply(ShortPath.class),scala.reflect.ClassTag$.MODULE$.apply(Integer.class));

        GraphOps ops = new GraphOps(G, scala.reflect.ClassTag$.MODULE$.apply(ShortPath.class),scala.reflect.ClassTag$.MODULE$.apply(Integer.class));

        ops.pregel(
                new ShortPath(Integer.MAX_VALUE),
                Integer.MAX_VALUE,
                EdgeDirection.Out(),
                new VProg(),
                new sendMsg(),
                new merge(),
                ClassTag$.MODULE$.apply(ShortPath.class))
                .vertices()
                .toJavaRDD()
                .sortBy(rdd -> ((Tuple2<Object, Integer>) rdd)._1, true, 1)
                .foreach(v -> {
                    Tuple2<Object,ShortPath> vertex = (Tuple2<Object,ShortPath>) v;
                    System.out.println("Minimum cost to get from "+labels.get(1L)+" to "+labels.get(vertex._1)+" is "
                            + vertex._2.getCost() + " through " + vertex._2.getPath().stream().map(labels::get).collect(Collectors.toList()).toString());
                });
    }
	
}
