package io.github.gabriel0liv.cpmconverter.validator;

public record CpmArtifactLimits(long maxArtifactBytes, int maxEntries, int maxEntryNameLength,
    long maxEntryUncompressedBytes, long maxTotalUncompressedBytes, int maxCompressionRatio,
    long maxConfigBytes, long maxAnimationJsonBytes, long maxSkinBytes, int maxJsonDepth,
    int maxStringLength, int maxNumberLength, int maxElements, int maxElementDepth,
    int maxAnimations, int maxFramesPerAnimation, int maxComponentsPerFrame, int maxPngChunks,
    long maxPngPixels, long maxPngDecodedBytes) {
  public CpmArtifactLimits {
    if (maxArtifactBytes <= 0 || maxEntries <= 0 || maxEntryNameLength <= 0 || maxEntryUncompressedBytes <= 0
        || maxTotalUncompressedBytes <= 0 || maxCompressionRatio <= 0 || maxConfigBytes <= 0
        || maxAnimationJsonBytes <= 0 || maxSkinBytes <= 0 || maxJsonDepth <= 0 || maxStringLength <= 0
        || maxNumberLength <= 0 || maxElements <= 0 || maxElementDepth <= 0 || maxAnimations <= 0
        || maxFramesPerAnimation <= 0 || maxComponentsPerFrame <= 0 || maxPngChunks <= 0
        || maxPngPixels <= 0 || maxPngDecodedBytes <= 0) throw new IllegalArgumentException("limits must be positive");
  }
  public CpmArtifactLimits(long a,int b,int c,long d,long e,int f,long g,long h,long i,int j,int k,int l,int m,int n,int o,int p,int q,int r){this(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,16_777_216L,64L*1024*1024);}
  public static CpmArtifactLimits defaults() { return new CpmArtifactLimits(32L*1024*1024,256,256,16L*1024*1024,64L*1024*1024,100,8L*1024*1024,8L*1024*1024,16L*1024*1024,128,1024*1024,128,50_000,256,128,100_000,50_000,4096,16_777_216L,64L*1024*1024); }
}
