import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { useAuth } from '@/hooks/useAuth';
import { supabase } from '@/integrations/supabase/client';
import { toast } from 'sonner';
import { User, Building2, X } from 'lucide-react';

const CompleteProfile = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const userType = searchParams.get('type') || 'student';
  
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<any>({
    name: '',
    location: '',
    skills: [],
    // Student specific
    qualification: '',
    sector_interest: '',
    category: '',
    experience_level: '',
    portfolio_url: '',
    linkedin_url: '',
    // Industry specific
    company_name: '',
    role: '',
    sector: '',
    required_skills: [],
    internship_capacity: 1
  });
  const [currentSkill, setCurrentSkill] = useState('');

  useEffect(() => {
    if (!user) {
      navigate(`/${userType}/auth`);
    }
  }, [user, navigate, userType]);

  const addSkill = (skillArray: string[], setSkillArray: (skills: string[]) => void) => {
    if (currentSkill.trim() && !skillArray.includes(currentSkill.trim())) {
      setSkillArray([...skillArray, currentSkill.trim()]);
      setCurrentSkill('');
    }
  };

  const removeSkill = (skill: string, skillArray: string[], setSkillArray: (skills: string[]) => void) => {
    setSkillArray(skillArray.filter(s => s !== skill));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setLoading(true);
    try {
      const profileData = {
        user_id: user.id,
        user_type: userType,
        name: formData.name,
        location: formData.location,
        skills: userType === 'student' ? formData.skills : undefined,
        qualification: userType === 'student' ? formData.qualification : undefined,
        sector_interest: userType === 'student' ? formData.sector_interest : undefined,
        category: userType === 'student' ? formData.category : undefined,
        experience_level: userType === 'student' ? formData.experience_level : undefined,
        portfolio_url: userType === 'student' ? formData.portfolio_url : undefined,
        linkedin_url: userType === 'student' ? formData.linkedin_url : undefined,
        company_name: userType === 'industry' ? formData.company_name : undefined,
        role: userType === 'industry' ? formData.role : undefined,
        sector: userType === 'industry' ? formData.sector : undefined,
        required_skills: userType === 'industry' ? formData.required_skills : undefined,
        internship_capacity: userType === 'industry' ? formData.internship_capacity : undefined
      };

      const { error } = await supabase.rpc('update_user_profile', {
        p_user_id: user.id,
        p_user_type: userType,
        p_name: profileData.name,
        p_location: profileData.location,
        p_skills: userType === 'student' ? profileData.skills : null,
        p_qualification: profileData.qualification,
        p_sector_interest: profileData.sector_interest,
        p_category: profileData.category,
        p_experience_level: profileData.experience_level,
        p_portfolio_url: profileData.portfolio_url,
        p_linkedin_url: profileData.linkedin_url,
        p_company_name: profileData.company_name,
        p_role: profileData.role,
        p_sector: profileData.sector,
        p_required_skills: userType === 'industry' ? profileData.required_skills : null,
        p_internship_capacity: profileData.internship_capacity
      });

      if (error) throw error;

      toast.success('Profile completed successfully!');
      navigate(`/${userType}/dashboard`);
    } catch (error: any) {
      console.error('Error completing profile:', error);
      toast.error('Failed to complete profile. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const isFormValid = () => {
    if (!formData.name || !formData.location) return false;
    
    if (userType === 'student') {
      return formData.qualification && formData.sector_interest;
    } else {
      return formData.company_name && formData.role && formData.sector;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/5 to-success/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-2xl">
        <CardHeader className="text-center">
          <div className="flex justify-center mb-4">
            {userType === 'student' ? (
              <User className="w-12 h-12 text-primary" />
            ) : (
              <Building2 className="w-12 h-12 text-primary" />
            )}
          </div>
          <CardTitle className="text-2xl">Complete Your Profile</CardTitle>
          <CardDescription>
            {userType === 'student' 
              ? 'Help us find the perfect internship matches for you'
              : 'Set up your company profile to find the best students'
            }
          </CardDescription>
        </CardHeader>
        
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Common Fields */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="name">
                  {userType === 'student' ? 'Full Name' : 'Contact Person Name'}
                </Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  placeholder={userType === 'student' ? 'Your full name' : 'Your name'}
                  required
                />
              </div>
              
              <div>
                <Label htmlFor="location">Location</Label>
                <Input
                  id="location"
                  value={formData.location}
                  onChange={(e) => setFormData({...formData, location: e.target.value})}
                  placeholder="City, State"
                  required
                />
              </div>
            </div>

            {/* Student Specific Fields */}
            {userType === 'student' && (
              <>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="qualification">Qualification</Label>
                    <Input
                      id="qualification"
                      value={formData.qualification}
                      onChange={(e) => setFormData({...formData, qualification: e.target.value})}
                      placeholder="e.g., B.Tech Computer Science"
                      required
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="experience_level">Experience Level</Label>
                    <Select value={formData.experience_level} onValueChange={(value) => setFormData({...formData, experience_level: value})}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select experience level" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="fresher">Fresher</SelectItem>
                        <SelectItem value="1-year">1 Year</SelectItem>
                        <SelectItem value="2-years">2 Years</SelectItem>
                        <SelectItem value="3+ years">3+ Years</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="sector_interest">Sector Interest</Label>
                    <Input
                      id="sector_interest"
                      value={formData.sector_interest}
                      onChange={(e) => setFormData({...formData, sector_interest: e.target.value})}
                      placeholder="e.g., Technology, Finance, Healthcare"
                      required
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="category">Category</Label>
                    <Select value={formData.category} onValueChange={(value) => setFormData({...formData, category: value})}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select category" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="development">Development</SelectItem>
                        <SelectItem value="design">Design</SelectItem>
                        <SelectItem value="marketing">Marketing</SelectItem>
                        <SelectItem value="finance">Finance</SelectItem>
                        <SelectItem value="operations">Operations</SelectItem>
                        <SelectItem value="research">Research</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="portfolio_url">Portfolio URL</Label>
                    <Input
                      id="portfolio_url"
                      type="url"
                      value={formData.portfolio_url}
                      onChange={(e) => setFormData({...formData, portfolio_url: e.target.value})}
                      placeholder="https://yourportfolio.com"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="linkedin_url">LinkedIn URL</Label>
                    <Input
                      id="linkedin_url"
                      type="url"
                      value={formData.linkedin_url}
                      onChange={(e) => setFormData({...formData, linkedin_url: e.target.value})}
                      placeholder="https://linkedin.com/in/yourprofile"
                    />
                  </div>
                </div>

                <div>
                  <Label>Skills</Label>
                  <div className="flex gap-2 mt-2">
                    <Input
                      value={currentSkill}
                      onChange={(e) => setCurrentSkill(e.target.value)}
                      placeholder="Add a skill"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          addSkill(formData.skills, (skills) => setFormData({...formData, skills}));
                        }
                      }}
                    />
                    <Button
                      type="button"
                      onClick={() => addSkill(formData.skills, (skills) => setFormData({...formData, skills}))}
                      variant="outline"
                    >
                      Add
                    </Button>
                  </div>
                  <div className="flex flex-wrap gap-2 mt-2">
                    {formData.skills.map((skill: string) => (
                      <Badge key={skill} variant="secondary" className="flex items-center gap-1">
                        {skill}
                        <X 
                          className="w-3 h-3 cursor-pointer"
                          onClick={() => removeSkill(skill, formData.skills, (skills) => setFormData({...formData, skills}))}
                        />
                      </Badge>
                    ))}
                  </div>
                </div>
              </>
            )}

            {/* Industry Specific Fields */}
            {userType === 'industry' && (
              <>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="company_name">Company Name</Label>
                    <Input
                      id="company_name"
                      value={formData.company_name}
                      onChange={(e) => setFormData({...formData, company_name: e.target.value})}
                      placeholder="Your company name"
                      required
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="role">Your Role</Label>
                    <Input
                      id="role"
                      value={formData.role}
                      onChange={(e) => setFormData({...formData, role: e.target.value})}
                      placeholder="e.g., HR Manager, Tech Lead"
                      required
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="sector">Company Sector</Label>
                    <Input
                      id="sector"
                      value={formData.sector}
                      onChange={(e) => setFormData({...formData, sector: e.target.value})}
                      placeholder="e.g., Technology, Finance, Healthcare"
                      required
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="internship_capacity">Internship Capacity</Label>
                    <Input
                      id="internship_capacity"
                      type="number"
                      min="1"
                      value={formData.internship_capacity}
                      onChange={(e) => setFormData({...formData, internship_capacity: parseInt(e.target.value) || 1})}
                      placeholder="Number of interns you can take"
                    />
                  </div>
                </div>

                <div>
                  <Label>Required Skills</Label>
                  <div className="flex gap-2 mt-2">
                    <Input
                      value={currentSkill}
                      onChange={(e) => setCurrentSkill(e.target.value)}
                      placeholder="Add a required skill"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          addSkill(formData.required_skills, (skills) => setFormData({...formData, required_skills: skills}));
                        }
                      }}
                    />
                    <Button
                      type="button"
                      onClick={() => addSkill(formData.required_skills, (skills) => setFormData({...formData, required_skills: skills}))}
                      variant="outline"
                    >
                      Add
                    </Button>
                  </div>
                  <div className="flex flex-wrap gap-2 mt-2">
                    {formData.required_skills.map((skill: string) => (
                      <Badge key={skill} variant="secondary" className="flex items-center gap-1">
                        {skill}
                        <X 
                          className="w-3 h-3 cursor-pointer"
                          onClick={() => removeSkill(skill, formData.required_skills, (skills) => setFormData({...formData, required_skills: skills}))}
                        />
                      </Badge>
                    ))}
                  </div>
                </div>
              </>
            )}

            <Button 
              type="submit" 
              className="w-full" 
              disabled={loading || !isFormValid()}
            >
              {loading ? 'Completing Profile...' : 'Complete Profile'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default CompleteProfile;