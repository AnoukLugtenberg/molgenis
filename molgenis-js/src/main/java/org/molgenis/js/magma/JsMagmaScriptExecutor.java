package org.molgenis.js.magma;

import java.util.Map;

import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.js.ScriptEvaluator;
import org.springframework.stereotype.Service;

/**
 * Executes a JavaScript using the Magma API
 */
@Service
public class JsMagmaScriptExecutor
{
	/**
	 * Execute a JavaScript using the Magma API
	 * 
	 * @param jsScript
	 * @param parameters
	 * @return
	 */
	public Object executeScript(String jsScript, Map<String, Object> parameters)
	{
		EntityMetaData entityMeta = new EntityMetaData("entity");
		parameters.keySet().stream().forEach(key -> {
			entityMeta.addAttribute(null); // FIXME
		});
		MapEntity entity = new MapEntity(parameters);
		return ScriptEvaluator.eval(jsScript, entity, entityMeta);
	}
}
