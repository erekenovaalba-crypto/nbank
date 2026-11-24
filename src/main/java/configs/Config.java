package configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    public static final String SERVER = "server";
    public static final String API_VERSION = "apiVersion";
    private static final String CONFIG_FILE = "config.properties";
    private static final Config INSTANCE = new Config();
    private final Properties properties = new Properties();
    private Config(){
        try(InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)){
            if (input == null){
                throw new RuntimeException(CONFIG_FILE + " not found in resources");
            }
            properties.load(input);
        } catch (IOException e){
            throw  new RuntimeException("Fail to load " + CONFIG_FILE, e);
        }
    }   public static String getProperty(String key){
        return INSTANCE.properties.getProperty(key);
    }
}

