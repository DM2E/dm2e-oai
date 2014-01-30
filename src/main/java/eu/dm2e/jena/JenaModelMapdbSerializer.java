package eu.dm2e.jena;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.mapdb.Serializer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class JenaModelMapdbSerializer implements Serializer<Model>, Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	@Override
	public Model deserialize(DataInput in, int available) throws IOException {
		byte[] buf = new byte[available];
		in.readFully(buf);
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		Model model = ModelFactory.createDefaultModel();
		model.read(bais, "", "N-TRIPLES");
		return model;
	}

	@Override
	public int fixedSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void serialize(DataOutput out, Model model) throws IOException {
		// TODO Auto-generated method stub
		OutputStream jenaOut = new ByteArrayOutputStream();
		model.write(jenaOut, "N-TRIPLES");
		byte[] buf = new byte[1024];
		ByteArrayInputStream jenaIn = new ByteArrayInputStream(buf);
		for (int read = jenaIn.read(); read > 0 ;) {
			out.write(buf);
		}
	}
	
}