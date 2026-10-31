/** 详情页封面：模糊放大的同图做背景，原图居中完整显示。参考 resources/模板详情与素材上传.png。 */
export function TemplateCover({ src, title }: { src: string; title: string }) {
  return (
    <div className="relative aspect-[16/10] overflow-hidden rounded-xl bg-surface-raised">
      <img
        src={src}
        alt=""
        aria-hidden="true"
        className="absolute inset-0 h-full w-full scale-110 object-cover opacity-50 blur-2xl"
      />
      <img
        src={src}
        alt={`${title}示例封面`}
        className="relative mx-auto h-full object-contain"
      />
    </div>
  );
}
