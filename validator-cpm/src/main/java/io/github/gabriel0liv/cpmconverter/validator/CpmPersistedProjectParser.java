package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Builds immutable persisted trees and registries from a validated JSON object. */
final class CpmPersistedProjectParser {
  private static final class Draft { final String name; final Long id; final String pointer; final int depth,index; final List<Draft> children=new ArrayList<>(); Draft(String n,Long i,String p,int d,int x){name=n;id=i;pointer=p;depth=d;index=x;} }
  CpmPersistedProjectV1 parse(JsonNode config, CpmPersistedSize2i validatedSize, CpmPngMetadata png, boolean texturePresent){
    var drafts=new ArrayList<Draft>(); var roots=new ArrayList<CpmPersistedRootV1>(); int[] next={0}; JsonNode elements=config.path("elements");
    for(int r=0;r<elements.size();r++){JsonNode root=elements.get(r); var children=new ArrayList<CpmPersistedElementV1>(); if(root.path("children").isArray()) for(int c=0;c<root.path("children").size();c++) children.add(freeze(read(root.path("children").get(c),"/elements/"+r+"/children/"+c,0,next,drafts))); roots.add(new CpmPersistedRootV1(root.path("id").asText(),children));}
    var flat=new ArrayList<CpmPersistedElementV1>(); var ids=new LinkedHashMap<Long,CpmPersistedElementV1>(); for(var root:roots) for(var child:root.children()) collect(child,flat,ids);
    var targets=new LinkedHashMap<Long,CpmPersistedTargetV1>(); for(var root:roots){long id=switch(root.id()){case "head"->0;case "body"->1;case "left_arm"->2;case "right_arm"->3;case "left_leg"->4;case "right_leg"->5;default->-1;}; if(id>=0) targets.put(id,new CpmPersistedRootTargetV1(root));} for(var e:flat) if(e.storeId()>=0) targets.put(e.storeId(),new CpmPersistedElementTargetV1(e));
    CpmPersistedTextureV1 texture = texturePresent && png != null
        ? new CpmPersistedTextureV1("skin.png", validatedSize,
            config.path("textures").path("skin").path("customGridSize").asBoolean(false),
            png) : null;
    return new CpmPersistedProjectV1(1,config.path("skinType").asText("default"),validatedSize,roots,flat,ids,targets,texture);
  }
  private Draft read(JsonNode n,String p,int depth,int[] next,List<Draft> order){var d=new Draft(n.path("name").asText(""),n.has("storeID")?n.path("storeID").longValue():null,p,depth,next[0]++); order.add(d); if(n.path("children").isArray()) for(int i=0;i<n.path("children").size();i++) d.children.add(read(n.path("children").get(i),p+"/children/"+i,depth+1,next,order)); return d;}
  private CpmPersistedElementV1 freeze(Draft d){var c=new ArrayList<CpmPersistedElementV1>(); for(var child:d.children)c.add(freeze(child)); return new CpmPersistedElementV1(d.name,d.id,c,d.index,d.depth,d.pointer);}
  private CpmPersistedElementV1 freeze(JsonNode n,String p,int depth,int[] next,List<Draft> order){return freeze(read(n,p,depth,next,order));}
  private void collect(CpmPersistedElementV1 e,List<CpmPersistedElementV1> flat,Map<Long,CpmPersistedElementV1> ids){flat.add(e); if(e.storeId()>=0)ids.put(e.storeId(),e); for(var c:e.children())collect(c,flat,ids);}
}
