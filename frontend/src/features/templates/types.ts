// 与后端 VO 字段一一对应（backend/src/main/java/com/xiaoying/template/*VO.java）

export type Category = {
  id: string;
  name: string;
};

export type TemplateCard = {
  id: string;
  categoryId: string;
  title: string;
  coverUrl: string;
  requiredImageCount: number;
};

export type TemplateDetail = {
  id: string;
  categoryId: string;
  categoryName: string;
  title: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
};
