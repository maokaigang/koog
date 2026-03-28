<!-- koog-zh-meta: {"last_synced_at": "2026-03-28T13:16:37+00:00", "source_path": "snippets/llm-parameters-snippets.md", "source_sha256": "82a09c3b302e5dba9397413fc43292af88c536e762c7511bedc790f077f7189c", "source_tag": "0.7.3", "translation_status": "changed"} -->
---
search:
  exclude: true
---


# --8<-- [start:heading]
| 参数 | 类型 | 说明 |
|------|------|------|

# --8<-- [end:heading]
# --8<-- [start:topP]
| `topP` | Double | 也称为核采样。通过将概率值最高的标记添加到子集中，直到其概率总和达到指定的 `topP` 值，从而创建下一个标记的子集。取值范围大于 0.0 且小于等于 1.0。 |

# --8<-- [end:topP]
# --8<-- [start:logprobs]
| `logprobs` | Boolean | 如果设置为 `true`，则包含输出标记的对数概率。 |

# --8<-- [end:logprobs]
# --8<-- [start:topLogprobs]
| `topLogprobs` | Integer | 每个位置最可能的前 N 个标记数量。取值范围为 0–20。需要将 `logprobs` 参数设置为 `true`。 |

# --8<-- [end:topLogprobs]
# --8<-- [start:frequencyPenalty]
| `frequencyPenalty` | Double | 对频繁出现的标记进行惩罚以减少重复。较高的 `frequencyPenalty` 值会导致措辞变化更大并减少重复。取值范围为 -2.0 到 2.0。 |

# --8<-- [end:frequencyPenalty]
# --8<-- [start:presencePenalty]
| `presencePenalty` | Double | 防止模型重用已包含在输出中的标记。较高的值鼓励引入新标记和新主题。取值范围为 -2.0 到 2.0。 |

# --8<-- [end:presencePenalty]
# --8<-- [start:stop]
| `stop` | List&lt;String&gt; | 指示模型在遇到其中任何一个字符串时应停止生成内容的字符串。例如，要使模型在生成两个换行符时停止生成内容，可将停止序列指定为 `stop = listOf("/n/n")`。 |

# --8<-- [end:stop]
# --8<-- [start:parallelToolCalls]
| `parallelToolCalls` | Boolean | 如果设置为 `true`，则可以并行运行多个工具调用。特别适用于自定义节点或代理策略之外的 LLM 交互。 |

# --8<-- [end:parallelToolCalls]
# --8<-- [start:promptCacheKey]
| `promptCacheKey` | String | 用于提示缓存的稳定缓存键。OpenAI 使用它来缓存类似请求的响应。 |

# --8<-- [end:promptCacheKey]
# --8<-- [start:safetyIdentifier]
| `safetyIdentifier` | String | 稳定且唯一的用户标识符，可用于检测违反 OpenAI 政策的用户。 |

# --8<-- [end:safetyIdentifier]
# --8<-- [start:serviceTier]
| `serviceTier` | ServiceTier | OpenAI 处理层级选择，允许您在性能与成本之间进行优先权衡。更多信息，请参阅 [ServiceTier](api:prompt-executor-openai-client-base::ai.koog.prompt.executor.clients.openai.base.models.ServiceTier) 的 API 文档。 |

# --8<-- [end:serviceTier]
# --8<-- [start:store]
| `store` | Boolean | 如果设置为 `true`，提供商可能会存储输出以供后续检索。 |

# --8<-- [end:store]
# --8<-- [start:audio]
| `audio` | OpenAIAudioConfig | 使用支持音频的模型时的音频输出配置。更多信息，请参阅 [OpenAIAudioConfig](api:prompt-executor-openai-client-base::ai.koog.prompt.executor.clients.openai.base.models.OpenAIAudioConfig) 的 API 文档。 |

# --8<-- [end:audio]
# --8<-- [start:reasoningEffort]
| `reasoningEffort` | ReasoningEffort | 指定模型将使用的推理努力级别。更多信息和可用值，请参阅 [ReasoningEffort](api:prompt-executor-openai-client-base::ai.koog.prompt.executor.clients.openai.base.models.ReasoningEffort) 的 API 文档。 |

# --8<-- [end:reasoningEffort]
# --8<-- [start:webSearchOptions]
| `webSearchOptions` | OpenAIWebSearchOptions | 配置网络搜索工具的使用（如果支持）。更多信息，请参阅 [OpenAIWebSearchOptions](api:prompt-executor-openai-client-base::ai.koog.prompt.executor.clients.openai.base.models.OpenAIWebSearchOptions) 的 API 文档。 |

# --8<-- [end:webSearchOptions]
# --8<-- [start:background]
| `background` | Boolean | 在后台运行响应。 |

# --8<-- [end:background]
# --8<-- [start:include]
| `include` | List&lt;OpenAIInclude&gt; | 要在模型响应中包含的额外数据，例如网络搜索工具调用的来源或文件搜索工具调用的搜索结果。详细的参考信息，请参阅 Koog API 参考中的 [OpenAIInclude](api:prompt-executor-openai-client::ai.koog.prompt.executor.clients.openai.models.OpenAIInclude)。要了解有关 `include` 参数的更多信息，请参阅 [OpenAI 的文档](https://platform.openai.com/docs/api-reference/responses/create#responses-create-include)。 |

# --8<-- [end:include]
# --8<-- [start:maxToolCalls]
| `maxToolCalls` | Integer | 此响应中允许的内置工具调用的最大总数。取值需大于或等于 `0`。 |

# --8<-- [end:maxToolCalls]
# --8<-- [start:reasoning]
| `reasoning` | ReasoningConfig | 适用于具备推理能力模型的推理配置。更多信息，请参阅 [ReasoningConfig](api:prompt-executor-openai-client::ai.koog.prompt.executor.clients.openai.models.ReasoningConfig) 的 API 文档。 |

# --8<-- [end:reasoning]
# --8<-- [start:truncation]
| `truncation` | Truncation | 当接近上下文窗口时的截断策略。更多信息，请参阅 [Truncation](api:prompt-executor-openai-client::ai.koog.prompt.executor.clients.openai.models.Truncation) 的 API 文档。 |

# --8<-- [end:truncation]
# --8<-- [start:topK]
| `topK` | Integer | 生成输出时考虑的最高概率令牌数量。取值需大于或等于 0（具体提供方可能有最低值要求）。 |

# --8<-- [end:topK]
# --8<-- [start:repetitionPenalty]
| `repetitionPenalty` | Double | 惩罚令牌重复。对于输出中已出现过的令牌，其下一个令牌的概率将除以 `repetitionPenalty` 的值，这使得如果 `repetitionPenalty > 1`，它们再次出现的可能性降低。取值需大于 0.0 且小于或等于 2.0。 |

# --8<-- [end:repetitionPenalty]
# --8<-- [start:minP]
| `minP` | Double | 过滤掉相对于最可能令牌的相对概率低于定义的 `minP` 值的令牌。取值范围为 0.0 至 0.1。 |

# --8<-- [end:minP]
# --8<-- [start:topA]
| `topA` | Double | 根据模型置信度动态调整采样窗口。如果模型置信度高（存在占主导地位的高概率下一个令牌），则采样窗口限制在少数几个最高概率令牌内。如果置信度低（存在许多概率相似的令牌），则保留更多令牌在采样窗口中。取值范围为 0.0 至 0.1（含）。值越高意味着动态适应性越强。 |

# --8<-- [end:topA]
# --8<-- [start:transforms]
| `transforms` | List&lt;String&gt; | 上下文转换列表。定义当上下文超出模型的令牌限制时如何转换上下文。默认转换是 `middle-out`，即从提示的中间截断。使用空列表表示不进行转换。更多信息，请参阅 OpenRouter 文档中的 [消息转换](https://openrouter.ai/docs/guides/features/message-transforms)。 |

# --8<-- [end:transforms]
# --8<-- [start:models]
| `models` | List&lt;String&gt; | 请求允许使用的模型列表。 |

# --8<-- [end:models]
# --8<-- [start:route]
| `route` | String | 请求使用的路由策略。 |

# --8<-- [end:route]
# --8<-- [start:provider]
| `provider` | ProviderPreferences | 包含一系列参数，用于显式控制 OpenRouter 如何选择 LLM 提供商。更多信息，请参阅 API 文档中的 [ProviderPreferences](api:prompt-executor-openrouter-client::ai.koog.prompt.executor.clients.openrouter.models.ProviderPreferences)。 |

# --8<-- [end:provider]
# --8<-- [start:stopSequences]
| `stopSequences` | List&lt;String&gt; | 导致模型停止生成内容的自定义文本序列。如果匹配，响应中 `stop_reason` 的值为 `stop_sequence`。 |

# --8<-- [end:stopSequences]
# --8<-- [start:container]
| `container`  | String | 跨请求重用的容器标识符。容器由 Anthropic 的代码执行工具使用，以提供安全且容器化的代码执行环境。通过提供先前响应中的容器标识符，您可以在多个请求间重用容器，从而在请求间保留已创建的文件。更多信息，请参阅 Anthropic 文档中的 [Containers](https://platform.claude.com/docs/en/agents-and-tools/tool-use/code-execution-tool#containers)。 |

# --8<-- [end:container]
# --8<-- [start:mcpServers]
| `mcpServers` | List&lt;AnthropicMCPServerURLDefinition&gt; | 请求中使用的 MCP 服务器定义。最多支持 20 个服务器。更多信息，请参阅 API 参考中的 [AnthropicMCPServerURLDefinition](api:prompt-executor-anthropic-client::ai.koog.prompt.executor.clients.anthropic.models.AnthropicMCPServerURLDefinition)。 |

# --8<-- [end:mcpServers]
# --8<-- [start:serviceTier]
| `serviceTier` | AnthropicServiceTier | 确定请求使用优先级容量（如果可用）还是标准容量。更多信息，请参阅 API 参考中的 [AnthropicServiceTier](api:prompt-executor-anthropic-client::ai.koog.prompt.executor.clients.anthropic.models.AnthropicServiceTier) 以及 Anthropic 的 [服务层级](https://platform.claude.com/docs/en/api/service-tiers) 文档。 |

# --8<-- [end:serviceTier]
# --8<-- [start:thinking]
| `thinking` | AnthropicThinking | 激活 Claude 扩展思考的配置。激活后，响应还会包含思考内容块。更多信息，请参阅 API 参考中的 [AnthropicThinking](api:prompt-executor-anthropic-client::ai.koog.prompt.executor.clients.anthropic.models.AnthropicThinking)。 |

# --8<-- [end:thinking]
# --8<-- [start:thinkingConfig]
| `thinkingConfig` | GoogleThinkingConfig | 控制模型是否应暴露其思维链以及可为其花费多少令牌。更多信息，请参阅 API 参考中的 [GoogleThinkingConfig](api:prompt-executor-google-client::ai.koog.prompt.executor.clients.google.models.GoogleThinkingConfig)。 |

# --8<-- [end:thinkingConfig]
# --8<-- [start:enableSearch]
| `enableSearch` | Boolean | 指定是否启用网络搜索功能。更多信息，请参阅阿里巴巴的 [网络搜索](https://www.alibabacloud.com/help/en/model-studio/web-search?spm=a2c63.p38356.0.i14) 文档。 |

# --8<-- [end:enableSearch]
# --8<-- [start:enableThinking]
| `enableThinking` | Boolean | 指定在使用混合思考模型时是否启用思考模式。更多信息，请参阅阿里巴巴关于 [深度思考](https://www.alibabacloud.com/help/en/model-studio/deep-thinking?spm=a2c63.p38356.0.i11) 的文档。 |

# --8<-- [end:enableThinking]
# --8<-- [start:randomSeed]
| `randomSeed` | 整数 | 用于随机采样的种子。若设置此值，相同参数与种子值的调用将产生确定性结果。 |

# --8<-- [end:randomSeed]
# --8<-- [start:promptMode]
| `promptMode` | 字符串 | 用于在推理模式与无系统提示之间切换。设为 `reasoning` 时，将使用推理模型的默认系统提示。更多信息请参阅 Mistral 的[推理](https://docs.mistral.ai/capabilities/reasoning)文档。 |

# --8<-- [end:promptMode]
# --8<-- [start:safePrompt]
| `safePrompt` | 布尔值 | 指定是否在所有对话前注入安全提示。安全提示用于实施防护机制，防范有害内容。更多信息请参阅 Mistral 的[审核与防护](https://docs.mistral.ai/capabilities/guardrailing)文档。 |

# --8<-- [end:safePrompt]