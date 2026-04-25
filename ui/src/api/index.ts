import {axiosInstance} from '@halo-dev/api-client'
import {
  CategoryV1alpha1Api,
  OverviewV1alpha1Api,
  SentencePublicV1alpha1Api,
  SentenceV1alpha1Api
} from './generated'

const categoryCoreApiClient = {
  category: new CategoryV1alpha1Api(undefined, "", axiosInstance),
};
const sentenceCoreApiClient = {
  sentence: new SentenceV1alpha1Api(undefined, "", axiosInstance),
};
const overviewV1alpha1ApiClient = {
  overview: new OverviewV1alpha1Api(undefined, "", axiosInstance),
}

const sentencePublicV1alpha1ApiClient = {
  sentence: new SentencePublicV1alpha1Api(undefined, "", axiosInstance),
}
export {
  categoryCoreApiClient,
  sentenceCoreApiClient,
  overviewV1alpha1ApiClient,
  sentencePublicV1alpha1ApiClient
};


