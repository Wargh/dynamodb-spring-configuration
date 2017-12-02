package au.com.wargh.env;

import static com.amazonaws.services.dynamodbv2.model.ComparisonOperator.EQ;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;

/**
 * Load properties from configuration dynamoDB table, registered in META-INF/spring.factories
 */
public class DynamodbEnvironmentPostProcessor implements EnvironmentPostProcessor, ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final DeferredLog log = new DeferredLog();

    private final AmazonDynamoDB amazonDynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment, SpringApplication application) {

        Boolean enabled = configurableEnvironment.getProperty("dynamodb.configuration.enabled", Boolean.class, true);
        
		if (enabled) {
            log.debug("Using DynamoDB configuration");

            String[] profiles = configurableEnvironment.getActiveProfiles();

	        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();

	        String table = configurableEnvironment.getProperty("dynamodb.configuration.table", "configuration");
	        
            propertySources.addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, loadDynamoDbConfiguration(table, profiles));

        } else {
            log.debug("Disabled DynamoDB configuration");
        }
    }

    private MapPropertySource loadDynamoDbConfiguration(String table, String... profiles) {
        amazonDynamoDBClient.describeTable(table);
		
		Map<String, Object> properties = new HashMap<String, Object>();
		
		properties.putAll(loadPropertiesForProfile(table, "default"));
		
		for (String profile : profiles) {
		    properties.putAll(loadPropertiesForProfile(table, profile));
		}
		
		return new MapPropertySource("dynamodb", properties);
    }

    private Map<String, Object> loadPropertiesForProfile(String tablename, String profile) {
        Map<String, Object> properties = new HashMap<String, Object>();
        
        QueryRequest request = new QueryRequest(tablename).withKeyConditions(profileOf(profile));

        QueryResult result;

        do {
            result = amazonDynamoDBClient.query(request);

            for (Map<String, AttributeValue> item : result.getItems()) {
                properties.put(item.get("key").getS(), item.get("value").getS());
            }

            request.setExclusiveStartKey(result.getLastEvaluatedKey());

        } while (result.getLastEvaluatedKey() != null);

        return properties;
    }

	private Map<String, Condition> profileOf(String profile) {
		Map<String, Condition> profileOf = new HashMap<>();
	
		profileOf.put("profile", when(EQ, profile));
		
		return profileOf;
	}

    private Condition when(ComparisonOperator operator, String value) {
        return new Condition().withComparisonOperator(operator).withAttributeValueList(new AttributeValue(value));
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        log.replayTo(DynamodbEnvironmentPostProcessor.class);
    }
}
