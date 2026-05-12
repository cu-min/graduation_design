import { useEffect, useMemo, useState } from 'react';
import { fetchTags } from '../api/metadata';
import { fetchProfileInterests, updateProfileInterests } from '../api/profile';
import type { TagOption } from '../types';
import { getErrorMessage } from '../utils/request';

type InterestOnboardingModalProps = {
  isOpen: boolean;
  onComplete: () => void;
};

function InterestOnboardingModal({ isOpen, onComplete }: InterestOnboardingModalProps) {
  const [allTags, setAllTags] = useState<TagOption[]>([]);
  const [selectedInterestIds, setSelectedInterestIds] = useState<number[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const groupedTags = useMemo(() => {
    const tagMap = new Map<number, TagOption[]>();
    for (const tag of allTags) {
      const current = tagMap.get(tag.categoryId) ?? [];
      current.push(tag);
      tagMap.set(tag.categoryId, current);
    }
    return Array.from(tagMap.entries());
  }, [allTags]);

  const selectedTags = useMemo(
    () => allTags.filter((tag) => selectedInterestIds.includes(tag.id)),
    [allTags, selectedInterestIds],
  );

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const loadOptions = async () => {
      setIsLoading(true);
      setErrorMessage('');

      try {
        const [tagResult, interestResult] = await Promise.all([fetchTags(), fetchProfileInterests()]);
        setAllTags(tagResult.data);
        setSelectedInterestIds(interestResult.data.map((tag) => tag.id));
      } catch (error) {
        setErrorMessage(getErrorMessage(error, '兴趣标签加载失败，请稍后重试'));
      } finally {
        setIsLoading(false);
      }
    };

    void loadOptions();
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleToggle = (tagId: number, checked: boolean) => {
    setSelectedInterestIds((current) =>
      checked ? Array.from(new Set([...current, tagId])) : current.filter((id) => id !== tagId),
    );
  };

  const handleSave = async () => {
    setIsSaving(true);
    setErrorMessage('');

    try {
      await updateProfileInterests({ tagIds: selectedInterestIds });
      window.dispatchEvent(new Event('app:interests-updated'));
      onComplete();
    } catch (error) {
      setErrorMessage(getErrorMessage(error, '兴趣标签保存失败，请稍后重试'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="modal-backdrop" role="presentation">
      <div
        className="auth-modal interest-onboarding-modal"
        role="dialog"
        aria-modal="true"
        aria-label="兴趣标签初始化设置"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="auth-header">
          <div>
            <h2>先选几个你感兴趣的主题</h2>
            <p className="modal-description">保存后首页会立刻刷新成更贴近你的推荐流。</p>
          </div>
        </div>

        {errorMessage ? <p className="auth-feedback error">{errorMessage}</p> : null}

        {selectedTags.length > 0 ? (
          <div className="profile-interest-preview-card onboarding-interest-preview">
            <strong>已选择 {selectedTags.length} 个标签</strong>
            <div className="news-tag-list">
              {selectedTags.map((tag) => (
                <span key={tag.id} className="news-tag">
                  {tag.name}
                </span>
              ))}
            </div>
          </div>
        ) : null}

        {isLoading ? (
          <div className="news-state-card compact-empty-state">正在加载兴趣标签...</div>
        ) : (
          <div className="interest-group-list onboarding-interest-list">
            {groupedTags.map(([categoryId, tags]) => (
              <div key={categoryId} className="interest-group-card">
                <div className="news-tag-list">
                  {tags.map((tag) => (
                    <label key={tag.id} className="tag-option profile-tag-option">
                      <input
                        type="checkbox"
                        checked={selectedInterestIds.includes(tag.id)}
                        onChange={(event) => handleToggle(tag.id, event.target.checked)}
                      />
                      <span>{tag.name}</span>
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="interest-onboarding-actions">
          <button
            type="button"
            className="primary-button"
            disabled={isSaving || selectedInterestIds.length === 0}
            onClick={() => void handleSave()}
          >
            {isSaving ? '保存中...' : '保存兴趣并刷新推荐'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default InterestOnboardingModal;
