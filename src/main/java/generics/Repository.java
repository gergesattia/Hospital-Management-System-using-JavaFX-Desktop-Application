package generics;

import java.util.ArrayList;
import java.util.List;

public class Repository<T> {
    private List<T > items = new ArrayList<>();
    
    public void add(T item) { 
        items.add(item); 
    }
    
    public List<T> getAll() { 
        return items; 
    }
    
    public T getById(int index) { 
        return items.get(index); 
    }
    
    public int size() { 
        return items.size(); 
    }
    
    public void clear() { 
        items.clear(); 
    }
}
