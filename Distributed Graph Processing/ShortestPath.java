package exercise_3;

import scala.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShortPath implements Serializable{
	
	private Integer cost;
	private List<Long> path;
	
	public ShortPath(Integer cost)
	{
		this.path = new ArrayList<>();
		this.cost = cost;
	}
	
	public ShortPath(Integer cost, List<Long> path)
	{
		this(cost);
        this.path.addAll(path);
	}
	
	public ShortPath(Integer cost, Long... path) 
	{
        this(cost, Arrays.asList(path));
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public List<Long> getPath() {
        return path;
    }

    public void setPath(List<Long> path) {
        this.path = path;
    }

    public void addToPath(Long id) {
        path.add(id);
    }

    @Override
    public String toString() {
        return String.format("{cost: %d, path: %s}", cost, path.toString());
    }
}
