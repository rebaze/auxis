package com.rebaze.auxis;

import com.rebaze.auxis.api.Indexer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AuxisIndexerApplicationTests {

	@Autowired
	Indexer indexer;

	@Test
	public void contextLoads() throws Exception {
		indexer.index(null);
	}

}
