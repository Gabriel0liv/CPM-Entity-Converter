package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Freezes the persisted JSON tree into immutable CPM records in true pre-order. */
final class CpmPersistedProjectParser {
  private record Node(String name, Long storeId, String pointer, List<Node> children) {}
  private record State(List<CpmPersistedElementV1> flat, Map<Long,CpmPersistedElementV1> ids, int[] index) {}
  CpmPersistedProjectV1 parse(JsonNode config, int width, int height, boolean texturePresent) {
    var roots = new ArrayList<CpmPersistedRootV1>(); var flat = new ArrayList<CpmPersistedElementV1>(); var ids = new LinkedHashMap<Long,CpmPersistedElementV1>(); var state = new State(flat,ids,new int[]{0});
    JsonNode elements=config.path("elements");
    for (JsonNode root: elements) { var children=new ArrayList<CpmPersistedElementV1>(); for (int i=0;i<root.path("children").size();i++) children.add(freeze(read(root.path("children").get(i),"/elements/"+root.path("id").asText()+"/children/"+i),0,state)); roots.add(new CpmPersistedRootV1(root.path("id").asText(),children)); }
    int sx=config.path("skinSize").path("x").asInt(texturePresent?64:1), sy=config.path("skinSize").path("y").asInt(texturePresent?64:1);
    return new CpmPersistedProjectV1(1,config.path("skinType").asText("default"),new CpmPersistedSize2i(Math.max(1,sx),Math.max(1,sy)),roots,flat,ids,new CpmPersistedTextureV1("skin.png",width,height,config.path("textures").path("skin").path("customGridSize").asBoolean(false)));
  }
  private Node read(JsonNode n,String pointer){var children=new ArrayList<Node>(); if(n.path("children").isArray()) for(int i=0;i<n.path("children").size();i++) children.add(read(n.path("children").get(i),pointer+"/children/"+i)); return new Node(n.path("name").asText(""),n.has("storeID")?n.path("storeID").longValue():null,pointer,children);}
  private CpmPersistedElementV1 freeze(Node n,int depth,State state){int index=state.index()[0]++; var child=new ArrayList<CpmPersistedElementV1>(); for(Node c:n.children()) child.add(freeze(c,depth+1,state)); var e=new CpmPersistedElementV1(n.name(),n.storeId(),child,index,depth,n.pointer()); state.flat().add(Math.min(index,state.flat().size()),e); if(n.storeId()!=null) state.ids().put(n.storeId(),e); return e;}
}
