package academy.aicode.astrobookings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class MinimalApp {
  public static void main(String[] args) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Rocket r = new Rocket("r-1", "Falcon", 5, 12345.67);
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(r);
    System.out.println(json);
  }

  public static class Rocket {
    public String id;
    public String name;
    public int capacity;
    public Double speed;

    public Rocket() {
    }

    public Rocket(String id, String name, int capacity, Double speed) {
      this.id = id;
      this.name = name;
      this.capacity = capacity;
      this.speed = speed;
    }
  }
}
